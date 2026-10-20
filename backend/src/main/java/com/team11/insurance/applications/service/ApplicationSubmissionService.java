package com.team11.insurance.applications.service;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStateMachine;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.applications.dto.SubmitApplicationRequest;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.kyc.IKycStatus;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;
import com.team11.insurance.underwriting.IUnderwriting;
import com.team11.insurance.underwriting.UnderwritingResult;

/**
 * Submit an insurance application (INS-F-007; SAD 4.1.2; contract docs/contracts/p3-contract.md 3.1).
 *
 * Flow: KYC check (403) -> documents present (422) -> quote belongs to the caller and is valid (404 / 422 / 409)
 * -> save as SUBMITTED in its own transaction -> underwriting -> move to APPROVED, REFERRED or REJECTED in a
 * second transaction -> audit entry. The method itself is NOT transactional, so the SUBMITTED row is committed
 * before underwriting runs, and a REJECTED application stays saved even though the controller then answers 422.
 *
 * Not done here yet (P3-04b, needs P4-06): checking that each document id exists in IFileStore, and the
 * APPLICATION_APPROVED / REFERRED / REJECTED notification.
 */
@Service
public class ApplicationSubmissionService {

    static final String ENTITY = "Application";
    static final int MAX_TEXT = 500;
    static final int MAX_REG_NO = 20;

    private final IKycStatus kycStatus;
    private final QuoteRepository quoteRepository;
    private final IPlanQuery planQuery;
    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final IUnderwriting underwriting;
    private final IAuditLog auditLog;
    private final ObjectMapper objectMapper;
    private final TransactionOperations tx;
    private final Clock clock;

    @Autowired
    public ApplicationSubmissionService(IKycStatus kycStatus, QuoteRepository quoteRepository,
                                        IPlanQuery planQuery, ApplicationRepository applicationRepository,
                                        ApplicationHistoryRepository historyRepository, IUnderwriting underwriting,
                                        IAuditLog auditLog, ObjectMapper objectMapper, TransactionOperations tx,
                                        ObjectProvider<Clock> clockProvider) {
        this(kycStatus, quoteRepository, planQuery, applicationRepository, historyRepository, underwriting,
                auditLog, objectMapper, tx, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    ApplicationSubmissionService(IKycStatus kycStatus, QuoteRepository quoteRepository, IPlanQuery planQuery,
                                 ApplicationRepository applicationRepository,
                                 ApplicationHistoryRepository historyRepository, IUnderwriting underwriting,
                                 IAuditLog auditLog, ObjectMapper objectMapper, TransactionOperations tx,
                                 Clock clock) {
        this.kycStatus = kycStatus;
        this.quoteRepository = quoteRepository;
        this.planQuery = planQuery;
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.underwriting = underwriting;
        this.auditLog = auditLog;
        this.objectMapper = objectMapper;
        this.tx = tx;
        this.clock = clock;
    }

    public SubmissionResult submit(CurrentUser user, SubmitApplicationRequest request) {
        long userId = user.getUserId();

        // 1. KYC must be verified. Nothing is saved before this check.
        if (!kycStatus.isVerified(userId)) {
            throw new AccessDeniedException("KYC verification is required before submitting an application.");
        }

        // 2. Supporting documents must be present (existence in the file store is checked in P3-04b).
        List<Long> documentIds = requireDocuments(request.documentIds());

        // 3. The quote must exist and belong to the caller (other customers' quotes look like 404).
        Quote quote = quoteRepository.findById(request.quoteId())
                .filter(q -> Objects.equals(q.getCustomerId(), userId))
                .orElseThrow(() -> new ResourceNotFoundException("Quote", request.quoteId()));
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(clock.instant())) {
            throw new ValidationException("The quote has expired. Please request a new quote.");
        }
        if (applicationRepository.existsByQuoteId(quote.getId())) {
            throw new StateConflictException("An application has already been submitted for this quote.");
        }
        Long insurerId = quote.getInsurerId() != null
                ? quote.getInsurerId() : planQuery.getPlan(quote.getPlanId()).getInsurerId();
        if (insurerId == null) {
            throw new ValidationException("The quote is not linked to an insurer.");
        }

        // 4. Save as SUBMITTED first (own transaction), with its first history row.
        Application submitted = Objects.requireNonNull(tx.execute(status -> {
            Application a = new Application();
            a.setCustomerId(userId);
            a.setInsurerId(insurerId);
            a.setPlanId(quote.getPlanId());
            a.setQuoteId(quote.getId());
            a.setProductType(quote.getProductType());
            a.setVehicleRegNo(vehicleRegNo(request.details()));
            a.setPremium(quote.getPremium());
            a.setDetailsJson(toJson(request.details()));
            a.setDocumentIds(documentIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            a.setStatus(ApplicationStatus.SUBMITTED);
            Application saved = applicationRepository.saveAndFlush(a);
            historyRepository.save(new ApplicationHistory(
                    saved.getId(), null, ApplicationStatus.SUBMITTED, userId, "Application submitted"));
            return saved;
        }));

        // 5. Underwriting runs outside any transaction; if it fails the application stays SUBMITTED.
        UnderwritingResult result = underwriting.evaluate(submitted);
        ApplicationStatus target = switch (result.getOutcome()) {
            case APPROVE -> ApplicationStatus.APPROVED;
            case REFER -> ApplicationStatus.REFERRED;
            case REJECT -> ApplicationStatus.REJECTED;
        };
        ApplicationStateMachine.requireTransition(ApplicationStatus.SUBMITTED, target);
        String reason = limit(result.getReason(), MAX_TEXT);

        // 6. Apply the outcome and its history row (second transaction).
        Long applicationId = submitted.getId();
        tx.executeWithoutResult(status -> {
            Application a = applicationRepository.findById(applicationId).orElseThrow(
                    () -> new ResourceNotFoundException("Application", applicationId));
            a.setStatus(target);
            if (target == ApplicationStatus.REJECTED) {
                a.setRejectionReason(reason);
            }
            applicationRepository.save(a);
            historyRepository.save(new ApplicationHistory(
                    applicationId, ApplicationStatus.SUBMITTED, target, null, reason));
        });

        // 7. Audit after the data is committed (the audit write has its own transaction).
        auditLog.record(userId, "APPLICATION_SUBMITTED", ENTITY, applicationId, "status=" + target);

        return new SubmissionResult(applicationId, target, reason);
    }

    private static List<Long> requireDocuments(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new ValidationException("A supporting document is required.", "documentIds");
        }
        if (documentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ValidationException("Every document id must be a positive number.", "documentIds");
        }
        return documentIds.stream().distinct().toList();
    }

    private static String vehicleRegNo(Map<String, Object> details) {
        Object value = details.get("vehicleRegNo");
        if (value instanceof String s && !s.isBlank()) {
            String regNo = s.trim().toUpperCase(Locale.ROOT);
            if (regNo.length() > MAX_REG_NO) {
                throw new ValidationException("vehicleRegNo is too long.", "vehicleRegNo");
            }
            return regNo;
        }
        return null;
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new ValidationException("The application details could not be read.", "details");
        }
    }

    private static String limit(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
