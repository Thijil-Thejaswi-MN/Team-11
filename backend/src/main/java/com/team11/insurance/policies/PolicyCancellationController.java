package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.claims.IClaimService;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.payments.IPaymentService;

/**
 * REST endpoints and workflow for Policy Cancellation & Refunds (P3-10 / INS-F-020).
 */
@RestController
public class PolicyCancellationController {

    private final PolicyRepository policyRepository;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final ObjectProvider<IClaimService> claimServiceProvider;
    private final ObjectProvider<IPaymentService> paymentServiceProvider;
    private final ObjectProvider<INotify> notifyProvider;
    private final ObjectProvider<IAuditLog> auditLogProvider;
    private final Clock clock;

    @Autowired
    public PolicyCancellationController(
            PolicyRepository policyRepository,
            CancellationRequestRepository cancellationRequestRepository,
            ObjectProvider<IClaimService> claimServiceProvider,
            ObjectProvider<IPaymentService> paymentServiceProvider,
            ObjectProvider<INotify> notifyProvider,
            ObjectProvider<IAuditLog> auditLogProvider,
            ObjectProvider<Clock> clockProvider) {
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
        this.cancellationRequestRepository = Objects.requireNonNull(cancellationRequestRepository, "cancellationRequestRepository must not be null");
        this.claimServiceProvider = claimServiceProvider;
        this.paymentServiceProvider = paymentServiceProvider;
        this.notifyProvider = notifyProvider;
        this.auditLogProvider = auditLogProvider;
        this.clock = clockProvider != null && clockProvider.getIfAvailable() != null
                ? clockProvider.getIfAvailable()
                : Clock.systemUTC();
    }

    public record CancellationSubmitRequest(String reason) {}

    public record CancellationSubmitResponse(
            Long requestId,
            Long policyId,
            String status,
            String reason
    ) {}

    public record CancellationDecisionRequest(String decision, String reason) {}

    public record CancellationDecisionResponse(
            Long requestId,
            Long policyId,
            String status,
            BigDecimal refundAmount
    ) {}

    public record CancellationSummaryDto(
            Long id,
            Long policyId,
            Long insurerId,
            Long requestedBy,
            String reason,
            CancellationStatus status,
            BigDecimal refundAmount,
            Instant createdAt,
            Instant decidedAt
    ) {}

    @PostMapping("/policies/{id}/cancellation")
    @Transactional
    public ResponseEntity<CancellationSubmitResponse> requestCancellation(
            @PathVariable Long id,
            @RequestBody(required = false) CancellationSubmitRequest request,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
        if (!Objects.equals(policy.getCustomerId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You are not authorized to cancel this policy.");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new StateConflictException("Only ACTIVE policies can be cancelled.");
        }

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Customer requested cancellation";
        if (reason.length() > 500) {
            throw new ValidationException("Cancellation reason must not exceed 500 characters.");
        }

        policy.setStatus(PolicyStatus.CANCELLATION_REQUESTED);
        policyRepository.save(policy);

        CancellationRequest cancelReq = new CancellationRequest();
        cancelReq.setPolicyId(policy.getId());
        cancelReq.setInsurerId(policy.getInsurerId());
        cancelReq.setRequestedBy(currentUser.getUserId());
        cancelReq.setReason(reason);
        cancelReq.setStatus(CancellationStatus.REQUESTED);

        CancellationRequest saved = cancellationRequestRepository.save(cancelReq);

        INotify notify = notifyProvider != null ? notifyProvider.getIfAvailable() : null;
        if (notify != null) {
            notify.send(
                    NotificationEvent.CANCELLATION_REQUESTED,
                    policy.getCustomerId(),
                    Map.of(
                            "policyNo", policy.getPolicyNo(),
                            "requestId", saved.getId() != null ? saved.getId().toString() : ""
                    )
            );
        }

        IAuditLog auditLog = auditLogProvider != null ? auditLogProvider.getIfAvailable() : null;
        if (auditLog != null && saved.getId() != null) {
            auditLog.record(
                    currentUser.getUserId(),
                    "CANCELLATION_REQUESTED",
                    "CancellationRequest",
                    saved.getId(),
                    reason
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CancellationSubmitResponse(saved.getId(), policy.getId(), "Requested", reason));
    }

    @GetMapping("/cancellations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CancellationSummaryDto>> listCancellations(
            @RequestParam(required = false) CancellationStatus status,
            CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserRole() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }

        List<CancellationSummaryDto> items = cancellationRequestRepository.findAll()
                .stream()
                .filter(req -> {
                    if (currentUser.getUserRole() == UserRole.CUSTOMER) {
                        return Objects.equals(req.getRequestedBy(), currentUser.getUserId());
                    }
                    if (currentUser.getUserRole() == UserRole.INSURER_ADMIN) {
                        return Objects.equals(req.getInsurerId(), currentUser.getUserId());
                    }
                    return currentUser.getUserRole() == UserRole.SYSTEM_ADMIN;
                })
                .filter(req -> status == null || req.getStatus() == status)
                .sorted(Comparator.comparing(CancellationRequest::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(req -> new CancellationSummaryDto(
                        req.getId(),
                        req.getPolicyId(),
                        req.getInsurerId(),
                        req.getRequestedBy(),
                        req.getReason(),
                        req.getStatus(),
                        req.getRefundAmount(),
                        req.getCreatedAt(),
                        req.getDecidedAt()
                ))
                .toList();

        return ResponseEntity.ok(items);
    }

    @PostMapping("/cancellations/{id}/decision")
    @Transactional
    public ResponseEntity<CancellationDecisionResponse> decideCancellation(
            @PathVariable Long id,
            @RequestBody CancellationDecisionRequest request,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.INSURER_ADMIN);

        if (request == null || request.decision() == null || request.decision().isBlank()) {
            throw new ValidationException("Decision ('Approve' or 'Reject') is required.");
        }

        CancellationRequest cancelReq = cancellationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation request not found: " + id));

        if (!Objects.equals(cancelReq.getInsurerId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You are not authorized to decide cancellation requests for another insurer.");
        }
        if (cancelReq.getStatus() != CancellationStatus.REQUESTED) {
            throw new StateConflictException("Cancellation request has already been decided.");
        }

        Policy policy = policyRepository.findById(cancelReq.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + cancelReq.getPolicyId()));

        String normalized = request.decision().trim().toUpperCase();
        boolean isApprove = "APPROVE".equals(normalized) || "APPROVED".equals(normalized);
        boolean isReject = "REJECT".equals(normalized) || "REJECTED".equals(normalized);

        if (!isApprove && !isReject) {
            throw new ValidationException("Decision must be 'Approve' or 'Reject'.");
        }

        if (isApprove) {
            IClaimService claimService = claimServiceProvider != null ? claimServiceProvider.getIfAvailable() : null;
            boolean claimPaid = claimService != null && claimService.hasPaidClaim(policy.getId());

            LocalDate cancelDate = cancelReq.getCreatedAt() != null
                    ? cancelReq.getCreatedAt().atZone(clock.getZone()).toLocalDate()
                    : LocalDate.now(clock);
            if (cancelDate.isBefore(policy.getStartDate())) {
                cancelDate = policy.getStartDate();
            } else if (cancelDate.isAfter(policy.getEndDate())) {
                cancelDate = policy.getEndDate();
            }

            BigDecimal refundAmount = RefundCalculator.calculateRefund(
                    policy.getPremiumPaid(),
                    policy.getStartDate(),
                    policy.getEndDate(),
                    cancelDate,
                    claimPaid
            );

            IPaymentService paymentService = paymentServiceProvider != null ? paymentServiceProvider.getIfAvailable() : null;
            if (paymentService != null) {
                String idempotencyKey = "cancel-" + policy.getId() + "-" + cancelReq.getId();
                paymentService.refund(policy.getId(), refundAmount, idempotencyKey, cancelReq.getReason());
            }

            cancelReq.setStatus(CancellationStatus.APPROVED);
            cancelReq.setDecidedBy(currentUser.getUserId());
            cancelReq.setDecidedAt(Instant.now(clock));
            cancelReq.setRefundAmount(refundAmount);
            cancellationRequestRepository.save(cancelReq);

            policy.setStatus(PolicyStatus.CANCELLED);
            policyRepository.save(policy);

            INotify notify = notifyProvider != null ? notifyProvider.getIfAvailable() : null;
            if (notify != null) {
                notify.send(
                        NotificationEvent.CANCELLATION_APPROVED,
                        policy.getCustomerId(),
                        Map.of(
                                "policyNo", policy.getPolicyNo(),
                                "refundAmount", refundAmount.toPlainString()
                        )
                );
            }

            IAuditLog auditLog = auditLogProvider != null ? auditLogProvider.getIfAvailable() : null;
            if (auditLog != null) {
                auditLog.record(
                        currentUser.getUserId(),
                        "CANCELLATION_APPROVED",
                        "CancellationRequest",
                        cancelReq.getId(),
                        "refundAmount=" + refundAmount.toPlainString()
                );
            }

            return ResponseEntity.ok(new CancellationDecisionResponse(
                    cancelReq.getId(),
                    policy.getId(),
                    "Approved",
                    refundAmount
            ));
        } else {
            cancelReq.setStatus(CancellationStatus.REJECTED);
            cancelReq.setDecidedBy(currentUser.getUserId());
            cancelReq.setDecidedAt(Instant.now(clock));
            cancelReq.setRefundAmount(BigDecimal.ZERO);
            cancellationRequestRepository.save(cancelReq);

            policy.setStatus(PolicyStatus.ACTIVE);
            policyRepository.save(policy);

            INotify notify = notifyProvider != null ? notifyProvider.getIfAvailable() : null;
            if (notify != null) {
                notify.send(
                        NotificationEvent.CANCELLATION_REJECTED,
                        policy.getCustomerId(),
                        Map.of(
                                "policyNo", policy.getPolicyNo(),
                                "reason", request.reason() != null ? request.reason() : "Rejected by insurer admin"
                        )
                );
            }

            IAuditLog auditLog = auditLogProvider != null ? auditLogProvider.getIfAvailable() : null;
            if (auditLog != null) {
                auditLog.record(
                        currentUser.getUserId(),
                        "CANCELLATION_REJECTED",
                        "CancellationRequest",
                        cancelReq.getId(),
                        request.reason() != null ? request.reason() : "Rejected by insurer admin"
                );
            }

            return ResponseEntity.ok(new CancellationDecisionResponse(
                    cancelReq.getId(),
                    policy.getId(),
                    "Rejected",
                    BigDecimal.ZERO
            ));
        }
    }
}
