package com.team11.insurance.policies;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.applications.Application;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.filestore.LoadedDocument;
import com.team11.insurance.filestore.StoredDocument;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.dto.PolicySummaryResponse;

/**
 * Service managing policy issuance, lifecycle queries, and PDF generation (P3-05 / INS-F-009, INS-F-024).
 */
@Service
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepository;
    private final PolicyPdfGenerator policyPdfGenerator;
    private final IFileStore fileStore;
    private final ObjectProvider<INotify> notifyProvider;
    private final ObjectProvider<PlanRepository> planRepositoryProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final Clock clock;

    @Autowired
    public PolicyService(
            PolicyRepository policyRepository,
            PolicyPdfGenerator policyPdfGenerator,
            IFileStore fileStore,
            ObjectProvider<INotify> notifyProvider,
            ObjectProvider<PlanRepository> planRepositoryProvider,
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<Clock> clockProvider) {
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
        this.policyPdfGenerator = Objects.requireNonNull(policyPdfGenerator, "policyPdfGenerator must not be null");
        this.fileStore = Objects.requireNonNull(fileStore, "fileStore must not be null");
        this.notifyProvider = notifyProvider;
        this.planRepositoryProvider = planRepositoryProvider;
        this.userRepositoryProvider = userRepositoryProvider;
        this.clock = clockProvider.getIfAvailable(Clock::systemDefaultZone);
    }

    public PolicyService(
            PolicyRepository policyRepository,
            PolicyPdfGenerator policyPdfGenerator,
            IFileStore fileStore,
            Clock clock) {
        this.policyRepository = policyRepository;
        this.policyPdfGenerator = policyPdfGenerator;
        this.fileStore = fileStore;
        this.notifyProvider = null;
        this.planRepositoryProvider = null;
        this.userRepositoryProvider = null;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    /**
     * Issues an active policy for an approved and paid application (INS-F-009, P3-05).
     * Idempotent: returns existing policy if already issued for this application.
     */
    @Transactional
    public Policy issuePolicy(Application application, BigDecimal premium) {
        Objects.requireNonNull(application, "application must not be null");
        Objects.requireNonNull(premium, "premium must not be null");

        // Idempotency: Issue once per application
        Optional<Policy> existingPolicy = policyRepository.findByApplicationId(application.getId());
        if (existingPolicy.isPresent()) {
            log.info("Policy already exists for application {}: {}", application.getId(), existingPolicy.get().getPolicyNo());
            return existingPolicy.get();
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusYears(1).minusDays(1);
        String policyNo = "POL-" + today.getYear() + "-" + String.format("%06d", application.getId());

        Policy policy = new Policy();
        policy.setPolicyNo(policyNo);
        policy.setApplicationId(application.getId());
        policy.setCustomerId(application.getCustomerId());
        policy.setInsurerId(application.getInsurerId());
        policy.setPlanId(application.getPlanId());
        policy.setVehicleRegNo(application.getVehicleRegNo());
        policy.setPremiumPaid(premium);
        policy.setStartDate(today);
        policy.setEndDate(endDate);
        policy.setStatus(PolicyStatus.ACTIVE);

        // Generate and store OpenPDF document in FileStore
        try {
            String planName = resolvePlanName(application.getPlanId());
            String customerName = resolveCustomerName(application.getCustomerId());
            byte[] pdfBytes = policyPdfGenerator.generatePolicyPdf(policy, planName, customerName);

            StoredDocument stored = fileStore.storeStream(
                    new ByteArrayInputStream(pdfBytes),
                    "policy-" + policyNo + ".pdf",
                    "application/pdf",
                    pdfBytes.length,
                    DocumentCategory.POLICY,
                    application.getCustomerId()
            );
            if (stored != null && stored.id() != null) {
                policy.setDocumentFileId(stored.id());
            }
        } catch (Exception e) {
            log.warn("Failed to generate/store PDF for policy {}: {}", policyNo, e.getMessage());
            // Do not fail policy activation if file store encounters error
        }

        Policy savedPolicy = policyRepository.save(policy);
        log.info("Successfully issued policy {} (ID: {}) for application {}",
                savedPolicy.getPolicyNo(), savedPolicy.getId(), application.getId());

        // Dispatch notification
        sendNotification(savedPolicy);

        return savedPolicy;
    }

    /**
     * Retrieves policies filtered by caller role and ownership (INS-F-024).
     */
    @Transactional(readOnly = true)
    public List<PolicySummaryResponse> getPolicies(CurrentUser currentUser, PolicyStatus status) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to access policies");
        }

        List<Policy> policies;

        if (currentUser.isCustomer()) {
            Long customerId = currentUser.getUserId();
            policies = (status != null)
                    ? policyRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, status)
                    : policyRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (currentUser.isInsurerAdmin() || currentUser.isUnderwriter()) {
            Long insurerId = currentUser.getInsurerId();
            if (insurerId == null) {
                policies = Collections.emptyList();
            } else {
                policies = (status != null)
                        ? policyRepository.findByInsurerIdAndStatusOrderByCreatedAtDesc(insurerId, status)
                        : policyRepository.findByInsurerIdOrderByCreatedAtDesc(insurerId);
            }
        } else if (currentUser.isSystemAdmin()) {
            policies = (status != null)
                    ? policyRepository.findByStatusOrderByCreatedAtDesc(status)
                    : policyRepository.findAllByOrderByCreatedAtDesc();
        } else {
            policies = Collections.emptyList();
        }

        return policies.stream()
                .map(PolicySummaryResponse::from)
                .toList();
    }

    /**
     * Retrieves a single policy ensuring role authorization.
     */
    @Transactional(readOnly = true)
    public PolicySummaryResponse getPolicyById(Long id, CurrentUser currentUser) {
        Policy policy = findAndAuthorizePolicy(id, currentUser);
        return PolicySummaryResponse.from(policy);
    }

    /**
     * Retrieves streamable policy document enforcing ownership.
     */
    @Transactional
    public LoadedDocument getPolicyDocument(Long id, CurrentUser currentUser) {
        Policy policy = findAndAuthorizePolicy(id, currentUser);

        // If documentFileId is not yet generated, generate it on demand
        if (policy.getDocumentFileId() == null) {
            log.info("Document not found for policy {}. Generating on demand...", policy.getPolicyNo());
            try {
                String planName = resolvePlanName(policy.getPlanId());
                String customerName = resolveCustomerName(policy.getCustomerId());
                byte[] pdfBytes = policyPdfGenerator.generatePolicyPdf(policy, planName, customerName);

                StoredDocument stored = fileStore.storeStream(
                        new ByteArrayInputStream(pdfBytes),
                        "policy-" + policy.getPolicyNo() + ".pdf",
                        "application/pdf",
                        pdfBytes.length,
                        DocumentCategory.POLICY,
                        policy.getCustomerId()
                );
                if (stored != null && stored.id() != null) {
                    policy.setDocumentFileId(stored.id());
                    policyRepository.save(policy);
                }
            } catch (Exception e) {
                log.error("Failed to generate on-demand PDF for policy {}: {}", policy.getPolicyNo(), e.getMessage());
                throw new ResourceNotFoundException("PolicyDocument", id);
            }
        }

        if (policy.getDocumentFileId() == null) {
            throw new ResourceNotFoundException("PolicyDocument", id);
        }

        return fileStore.get(policy.getDocumentFileId(), currentUser);
    }

    private Policy findAndAuthorizePolicy(Long id, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to access policy " + id);
        }

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", id));

        if (currentUser.isCustomer()) {
            if (!Objects.equals(policy.getCustomerId(), currentUser.getUserId())) {
                throw new AccessDeniedException("You do not have permission to access policy " + id);
            }
        } else if (currentUser.isInsurerAdmin() || currentUser.isUnderwriter()) {
            if (!Objects.equals(policy.getInsurerId(), currentUser.getInsurerId())) {
                throw new AccessDeniedException("Insurer mismatch: You cannot access policy " + id);
            }
        } else if (!currentUser.isSystemAdmin()) {
            throw new AccessDeniedException("Access denied to policy " + id);
        }

        return policy;
    }

    private String resolvePlanName(Long planId) {
        if (planId == null || planRepositoryProvider == null) return null;
        PlanRepository repo = planRepositoryProvider.getIfAvailable();
        if (repo == null) return null;
        return repo.findById(planId).map(Plan::getName).orElse(null);
    }

    private String resolveCustomerName(Long customerId) {
        if (customerId == null || userRepositoryProvider == null) return null;
        UserRepository repo = userRepositoryProvider.getIfAvailable();
        if (repo == null) return null;
        return repo.findById(customerId).map(User::getName).orElse(null);
    }

    private void sendNotification(Policy policy) {
        if (notifyProvider == null) return;
        INotify notify = notifyProvider.getIfAvailable();
        if (notify != null) {
            try {
                notify.send(
                        NotificationEvent.POLICY_ISSUED,
                        policy.getCustomerId(),
                        Map.of(
                                "policyNo", policy.getPolicyNo(),
                                "premium", policy.getPremiumPaid() != null ? policy.getPremiumPaid().toPlainString() : "0.00"
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch POLICY_ISSUED notification: {}", e.getMessage());
            }
        }
    }
}
