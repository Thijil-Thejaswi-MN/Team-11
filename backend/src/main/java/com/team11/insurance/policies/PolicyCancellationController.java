package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.auth.CurrentUser;
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
 * Policy Cancellation & Refund REST Controller (P3-10 / INS-F-020).
 */
@RestController
@RequestMapping("/api/v1")
public class PolicyCancellationController {

    private final PolicyRepository policyRepository;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final IClaimService claimService;
    private final IPaymentService paymentService;
    private final INotify notifyService;
    private final IAuditLog auditLog;
    private final Clock clock;

    public PolicyCancellationController(
            PolicyRepository policyRepository,
            CancellationRequestRepository cancellationRequestRepository,
            @Qualifier("claimService") ObjectProvider<IClaimService> claimServiceProvider,
            ObjectProvider<IPaymentService> paymentServiceProvider,
            ObjectProvider<INotify> notifyServiceProvider,
            ObjectProvider<IAuditLog> auditLogProvider,
            ObjectProvider<Clock> clockProvider) {
        this.policyRepository = policyRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.claimService = resolveClaimService(claimServiceProvider);
        this.paymentService = paymentServiceProvider.getIfAvailable();
        this.notifyService = notifyServiceProvider.getIfAvailable();
        this.auditLog = auditLogProvider.getIfAvailable();
        Clock providedClock = clockProvider.getIfAvailable();
        this.clock = providedClock != null ? providedClock : Clock.systemDefaultZone();
    }

    private static IClaimService resolveClaimService(ObjectProvider<IClaimService> provider) {
        if (provider == null) {
            return null;
        }
        try {
            return provider.getIfAvailable();
        } catch (NoUniqueBeanDefinitionException ex) {
            return provider.orderedStream()
                    .filter(b -> !b.getClass().getSimpleName().toLowerCase().contains("stub"))
                    .findFirst()
                    .orElseGet(() -> provider.orderedStream().findFirst().orElse(null));
        }
    }

    @PostMapping("/policies/{id}/cancellation")
    @Transactional
    public ResponseEntity<CancellationSubmitResponse> requestCancellation(
            @PathVariable Long id,
            @RequestBody(required = false) CancellationSubmitRequest request,
            CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("Only customers can request policy cancellation.");
        }

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
        if (!Objects.equals(policy.getCustomerId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to cancel this policy.");
        }

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new StateConflictException("Only Active policies can be cancelled (current status: " + policy.getStatus() + ").");
        }

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Customer requested policy cancellation";

        CancellationRequest cancelReq = new CancellationRequest();
        cancelReq.setPolicyId(id);
        cancelReq.setInsurerId(policy.getInsurerId());
        cancelReq.setRequestedBy(currentUser.getUserId());
        cancelReq.setReason(reason);
        cancelReq.setStatus(CancellationStatus.REQUESTED);
        CancellationRequest saved = cancellationRequestRepository.save(cancelReq);

        policy.setStatus(PolicyStatus.CANCELLATION_REQUESTED);
        policyRepository.save(policy);

        Long reqId = saved != null ? saved.getId() : null;
        if (auditLog != null) {
            auditLog.record(currentUser.getUserId(), "CANCELLATION_REQUESTED", "Policy", id,
                    "Cancellation requested: reason=" + reason);
        }
        if (notifyService != null) {
            notifyService.send(NotificationEvent.CANCELLATION_REQUESTED, policy.getCustomerId(), Map.of(
                    "policyNo", policy.getPolicyNo(),
                    "reason", reason
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CancellationSubmitResponse(reqId, "Requested"));
    }

    @GetMapping("/cancellations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CancellationRequest>> listCancellations(
            @RequestParam(required = false) CancellationStatus status,
            CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserRole() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }

        List<CancellationRequest> all = cancellationRequestRepository.findAll();
        List<CancellationRequest> list;
        if (currentUser.getUserRole() == UserRole.CUSTOMER) {
            list = all.stream()
                    .filter(r -> Objects.equals(r.getRequestedBy(), currentUser.getUserId()))
                    .filter(r -> status == null || r.getStatus() == status)
                    .toList();
        } else if (currentUser.getUserRole() == UserRole.INSURER_ADMIN) {
            Long insurerId = currentUser.getInsurerId();
            if (insurerId == null) {
                throw new AccessDeniedException("Insurer Admin context is missing insurerId.");
            }
            list = all.stream()
                    .filter(r -> Objects.equals(r.getInsurerId(), insurerId))
                    .filter(r -> status == null || r.getStatus() == status)
                    .toList();
        } else if (currentUser.getUserRole() == UserRole.SYSTEM_ADMIN) {
            list = all.stream()
                    .filter(r -> status == null || r.getStatus() == status)
                    .toList();
        } else {
            throw new AccessDeniedException("Access denied for role: " + currentUser.getUserRole());
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/cancellations/{id}/decision")
    @Transactional
    public ResponseEntity<CancellationDecisionResponse> decideCancellation(
            @PathVariable Long id,
            @RequestBody CancellationDecisionRequest request,
            CurrentUser currentUser) {
        if (currentUser == null || (currentUser.getUserRole() != UserRole.INSURER_ADMIN
                && currentUser.getUserRole() != UserRole.SYSTEM_ADMIN)) {
            throw new AccessDeniedException("Only Insurer Admin can decide cancellation requests.");
        }
        if (request == null || request.decision() == null || request.decision().isBlank()) {
            throw new ValidationException("Field 'decision' is required ('Approve' or 'Reject').");
        }

        CancellationRequest cancelReq = cancellationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancellation request not found: " + id));

        if (currentUser.getUserRole() == UserRole.INSURER_ADMIN
                && !Objects.equals(currentUser.getInsurerId(), cancelReq.getInsurerId())) {
            throw new AccessDeniedException("Cannot decide cancellation request belonging to another insurer.");
        }

        if (cancelReq.getStatus() != CancellationStatus.REQUESTED) {
            throw new StateConflictException("Cancellation request has already been decided: " + cancelReq.getStatus());
        }

        Policy policy = policyRepository.findById(cancelReq.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + cancelReq.getPolicyId()));

        String decisionNorm = request.decision().trim();
        Instant now = Instant.now(clock);
        LocalDate decisionDate = LocalDate.now(clock);

        if ("Approve".equalsIgnoreCase(decisionNorm) || "Approved".equalsIgnoreCase(decisionNorm)) {
            Long polId = policy.getId() != null ? policy.getId() : cancelReq.getPolicyId();
            boolean hasSettledClaim = claimService != null && claimService.hasPaidClaim(polId);

            LocalDate effectiveCancelDate = decisionDate;
            if (effectiveCancelDate.isBefore(policy.getStartDate())) {
                effectiveCancelDate = policy.getStartDate();
            } else if (effectiveCancelDate.isAfter(policy.getEndDate())) {
                effectiveCancelDate = policy.getEndDate();
            }

            BigDecimal refundAmount = RefundCalculator.calculateRefund(
                    policy.getPremiumPaid(),
                    policy.getStartDate(),
                    policy.getEndDate(),
                    effectiveCancelDate,
                    hasSettledClaim
            );

            if (paymentService != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.refund(
                        polId != null ? polId : id,
                        refundAmount,
                        "cancel-" + id,
                        cancelReq.getReason() != null ? cancelReq.getReason() : "Approved cancellation"
                );
            }

            cancelReq.setStatus(CancellationStatus.APPROVED);
            cancelReq.setRefundAmount(refundAmount);
            cancelReq.setDecidedBy(currentUser.getUserId());
            cancelReq.setDecidedAt(now);
            cancellationRequestRepository.save(cancelReq);

            policy.setStatus(PolicyStatus.CANCELLED);
            policyRepository.save(policy);

            if (auditLog != null) {
                auditLog.record(currentUser.getUserId(), "CANCELLATION_APPROVED", "Policy", polId,
                        "Approved cancellation request id=" + id + ", refundAmount=" + refundAmount);
            }
            if (notifyService != null) {
                notifyService.send(NotificationEvent.CANCELLATION_APPROVED, policy.getCustomerId(), Map.of(
                        "policyNo", policy.getPolicyNo(),
                        "refundAmount", refundAmount.toPlainString()
                ));
            }

            return ResponseEntity.ok(new CancellationDecisionResponse("Approved", refundAmount));
        } else if ("Reject".equalsIgnoreCase(decisionNorm) || "Rejected".equalsIgnoreCase(decisionNorm)) {
            Long polId = policy.getId() != null ? policy.getId() : cancelReq.getPolicyId();
            cancelReq.setStatus(CancellationStatus.REJECTED);
            cancelReq.setRefundAmount(BigDecimal.ZERO);
            cancelReq.setDecidedBy(currentUser.getUserId());
            cancelReq.setDecidedAt(now);
            cancellationRequestRepository.save(cancelReq);

            policy.setStatus(PolicyStatus.ACTIVE);
            policyRepository.save(policy);

            if (auditLog != null) {
                auditLog.record(currentUser.getUserId(), "CANCELLATION_REJECTED", "Policy", polId,
                        "Rejected cancellation request id=" + id);
            }
            if (notifyService != null) {
                notifyService.send(NotificationEvent.CANCELLATION_REJECTED, policy.getCustomerId(), Map.of(
                        "policyNo", policy.getPolicyNo(),
                        "reason", request.reason() != null ? request.reason() : "Cancellation request rejected"
                ));
            }

            return ResponseEntity.ok(new CancellationDecisionResponse("Rejected", BigDecimal.ZERO));
        } else {
            throw new ValidationException("Invalid decision value: " + request.decision() + ". Expected 'Approve' or 'Reject'.");
        }
    }

    public record CancellationSubmitRequest(String reason) {}

    public record CancellationSubmitResponse(Long requestId, String status) {}

    public record CancellationDecisionRequest(String decision, String reason) {}

    public record CancellationDecisionResponse(String status, BigDecimal refundAmount) {}
}
