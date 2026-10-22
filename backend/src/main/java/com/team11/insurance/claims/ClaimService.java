package com.team11.insurance.claims;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;
import com.team11.insurance.policies.PolicyView;

/**
 * Claims Core Workflow Service Implementation (Component C7 - SAD §4.1.3, §4.3.5, P4 Contract Section 5).
 */
@Service
@Transactional
public class ClaimService implements IClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    private final ClaimRepository claimRepository;
    private final ClaimHistoryRepository claimHistoryRepository;
    private final ObjectProvider<IPolicyQuery> policyQueryProvider;
    private final ObjectProvider<PolicyRepository> policyRepositoryProvider;
    private final ObjectProvider<INotify> notifyProvider;
    private final Clock clock;
    private final AtomicLong claimSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    public ClaimService(
            ClaimRepository claimRepository,
            ClaimHistoryRepository claimHistoryRepository,
            ObjectProvider<IPolicyQuery> policyQueryProvider,
            ObjectProvider<PolicyRepository> policyRepositoryProvider,
            ObjectProvider<INotify> notifyProvider,
            ObjectProvider<Clock> clockProvider) {
        this.claimRepository = claimRepository;
        this.claimHistoryRepository = claimHistoryRepository;
        this.policyQueryProvider = policyQueryProvider;
        this.policyRepositoryProvider = policyRepositoryProvider;
        this.notifyProvider = notifyProvider;
        this.clock = clockProvider != null
                ? clockProvider.getIfAvailable(Clock::systemDefaultZone)
                : Clock.systemDefaultZone();
    }

    // Constructor for testing with explicit clock and dependencies
    public ClaimService(
            ClaimRepository claimRepository,
            ClaimHistoryRepository claimHistoryRepository,
            IPolicyQuery policyQuery,
            PolicyRepository policyRepository,
            INotify notify,
            Clock clock) {
        this.claimRepository = claimRepository;
        this.claimHistoryRepository = claimHistoryRepository;
        this.policyQueryProvider = new SimpleObjectProvider<>(policyQuery);
        this.policyRepositoryProvider = new SimpleObjectProvider<>(policyRepository);
        this.notifyProvider = new SimpleObjectProvider<>(notify);
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    /**
     * Checks if any claim has been settled on a policy (Decision D-09.5).
     * Returns true ONLY if at least one claim has status SETTLED.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasPaidClaim(Long policyId) {
        if (policyId == null) {
            return false;
        }
        return claimRepository.existsByPolicyIdAndStatus(policyId, ClaimStatus.SETTLED);
    }

    /**
     * Files a new claim against an active policy within 30 days of incident.
     */
    @Override
    public ClaimResponse file(ClaimSubmissionRequest request, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        if (request == null) {
            throw new ValidationException("Claim submission request cannot be null.");
        }
        if (request.policyId() == null) {
            throw new ValidationException("Policy ID is required.");
        }
        if (request.incidentDate() == null) {
            throw new ValidationException("Incident date is required.");
        }
        if (request.claimedAmount() == null || request.claimedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Claimed amount must be greater than zero.");
        }

        // 1. Validate that the policy exists
        PolicyInfo policy = getPolicyInfo(request.policyId());

        // 2. Validate that the policy is ACTIVE
        if (policy.status() != PolicyStatus.ACTIVE) {
            throw new StateConflictException("Policy is not active (current status: " + policy.status() + ").");
        }

        // 3. Validate that the policy belongs to the authenticated customer
        if (!currentUser.getUserId().equals(policy.customerId())) {
            throw new AccessDeniedException("Access denied: policy does not belong to authenticated customer.");
        }

        // 4. Validate incident date boundary (within 30 days, not in future, within policy term)
        LocalDate today = LocalDate.now(clock);
        LocalDate incidentDate = request.incidentDate();

        if (incidentDate.isAfter(today)) {
            throw new ValidationException("Incident date cannot be in the future.");
        }
        if (incidentDate.isBefore(today.minusDays(30))) {
            throw new ValidationException("Incident date must be within 30 days of filing (incident date: "
                    + incidentDate + ", minimum allowed: " + today.minusDays(30) + ").");
        }
        if (incidentDate.isBefore(policy.startDate()) || incidentDate.isAfter(policy.endDate())) {
            throw new ValidationException("Incident date " + incidentDate
                    + " is outside the policy coverage period (" + policy.startDate() + " to " + policy.endDate() + ").");
        }

        // 5. Duplicate detection (UC-24 / SRS INS-F-023)
        // Matching criteria: same policyId, incidentDate, and claimAmount. Filing is NOT blocked.
        boolean isDuplicate = claimRepository.existsByPolicyIdAndIncidentDateAndClaimAmount(
                policy.id(), incidentDate, request.claimedAmount());

        // 6. Generate claim number
        String claimNo = generateClaimNo(today.getYear());

        // 7. Persist claim in SUBMITTED
        Claim claim = new Claim();
        claim.setClaimNo(claimNo);
        claim.setPolicyId(policy.id());
        claim.setPolicyNo(policy.policyNo());
        claim.setCustomerId(policy.customerId());
        claim.setInsurerId(policy.insurerId());
        claim.setIncidentDate(incidentDate);
        claim.setClaimAmount(request.claimedAmount());
        claim.setDescription(request.description());
        claim.setDocumentIds(request.documentIds() != null ? request.documentIds() : List.of());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setDuplicateFlag(isDuplicate);

        claim = claimRepository.saveAndFlush(claim);

        // Record initial history
        claimHistoryRepository.save(new ClaimHistory(
                claim.getId(), null, ClaimStatus.SUBMITTED, currentUser.getUserId(), "Claim filed by customer"));

        // 8. Canonical workflow transition: SUBMITTED -> DOCUMENT_VERIFICATION
        ClaimStateMachine.requireTransition(claim.getStatus(), ClaimStatus.DOCUMENT_VERIFICATION);
        claim.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);
        claim = claimRepository.saveAndFlush(claim);

        claimHistoryRepository.save(new ClaimHistory(
                claim.getId(), ClaimStatus.SUBMITTED, ClaimStatus.DOCUMENT_VERIFICATION, null,
                isDuplicate ? "Automated workflow intake (possible duplicate flagged)"
                            : "Automated workflow intake into document verification"));

        // 9. Emit CLAIM_RECEIVED notification through INotify
        emitNotification(NotificationEvent.CLAIM_RECEIVED, claim.getCustomerId(), Map.of(
                "claimNo", claim.getClaimNo(),
                "policyNo", claim.getPolicyNo() != null ? claim.getPolicyNo() : "",
                "claimedAmount", claim.getClaimAmount().toPlainString()
        ));

        String message = isDuplicate
                ? "Claim submitted and under document verification (flagged as possible duplicate)"
                : "Claim submitted successfully and under document verification";

        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNo(),
                claim.getPolicyId(),
                claim.getStatus(),
                claim.getClaimAmount(),
                claim.getAssessedAmount(),
                claim.isDuplicateFlag(),
                message
        );
    }

    /**
     * Retrieves claim details and status history.
     * Customers may access their own claims.
     * Claims Officers may access claims only for their own insurer.
     * Fails closed when CurrentUser is missing.
     */
    @Override
    @Transactional(readOnly = true)
    public ClaimView get(Long claimId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (claimId == null) {
            throw new ResourceNotFoundException("Claim", null);
        }

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        assertAccess(currentUser, claim);

        return toClaimView(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimView> list(Long policyId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (policyId == null) {
            return List.of();
        }

        PolicyInfo policy = getPolicyInfo(policyId);
        if (currentUser.isCustomer()) {
            if (!currentUser.getUserId().equals(policy.customerId())) {
                throw new AccessDeniedException("Access denied: policy does not belong to customer.");
            }
        } else if (currentUser.isStaff()) {
            RoleSecurity.assertInsurerAccess(currentUser, policy.insurerId());
        }

        return claimRepository.findByPolicyId(policyId).stream()
                .map(this::toClaimView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimView> listForInsurer(Long insurerId, ClaimStatus status, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        RoleSecurity.assertRole(currentUser, UserRole.CLAIMS_OFFICER, UserRole.INSURER_ADMIN, UserRole.SYSTEM_ADMIN);
        RoleSecurity.assertInsurerAccess(currentUser, insurerId);

        List<Claim> claims = (status != null)
                ? claimRepository.findByInsurerIdAndStatus(insurerId, status)
                : claimRepository.findByInsurerId(insurerId);

        return claims.stream()
                .map(this::toClaimView)
                .toList();
    }

    @Override
    public ClaimResponse verify(Long claimId, ClaimVerificationRequest request, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        RoleSecurity.assertRole(currentUser, UserRole.CLAIMS_OFFICER, UserRole.INSURER_ADMIN);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        RoleSecurity.assertInsurerAccess(currentUser, claim.getInsurerId());

        if (request == null) {
            throw new ValidationException("Verification request cannot be null.");
        }

        if (request.documentsComplete()) {
            ClaimStateMachine.requireTransition(claim.getStatus(), ClaimStatus.ASSESSMENT);
            ClaimStatus previousStatus = claim.getStatus();
            claim.setStatus(ClaimStatus.ASSESSMENT);
            claim = claimRepository.saveAndFlush(claim);

            claimHistoryRepository.save(new ClaimHistory(
                    claim.getId(), previousStatus, ClaimStatus.ASSESSMENT, currentUser.getUserId(),
                    request.notes() != null ? request.notes() : "Documents verified complete"));

            return new ClaimResponse(
                    claim.getId(), claim.getClaimNo(), claim.getPolicyId(), claim.getStatus(),
                    claim.getClaimAmount(), claim.getAssessedAmount(), claim.isDuplicateFlag(),
                    "Claim documents verified; moved to Assessment"
            );
        } else {
            // Remains in DOCUMENT_VERIFICATION
            ClaimStateMachine.requireTransition(claim.getStatus(), ClaimStatus.DOCUMENT_VERIFICATION);
            String missingDocs = request.missing() != null ? String.join(", ", request.missing()) : "Additional documents required";
            claimHistoryRepository.save(new ClaimHistory(
                    claim.getId(), claim.getStatus(), ClaimStatus.DOCUMENT_VERIFICATION, currentUser.getUserId(),
                    "Documents requested: " + missingDocs));

            emitNotification(NotificationEvent.CLAIM_DOCUMENTS_REQUESTED, claim.getCustomerId(), Map.of(
                    "claimNo", claim.getClaimNo(),
                    "missing", missingDocs
            ));

            return new ClaimResponse(
                    claim.getId(), claim.getClaimNo(), claim.getPolicyId(), claim.getStatus(),
                    claim.getClaimAmount(), claim.getAssessedAmount(), claim.isDuplicateFlag(),
                    "Additional documents requested; remains in Document Verification"
            );
        }
    }

    @Override
    public ClaimResponse decide(Long claimId, ClaimDecisionRequest request, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        RoleSecurity.assertRole(currentUser, UserRole.CLAIMS_OFFICER, UserRole.INSURER_ADMIN);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        RoleSecurity.assertInsurerAccess(currentUser, claim.getInsurerId());

        if (request == null) {
            throw new ValidationException("Decision request cannot be null.");
        }
        if (request.decision() == null || request.decision().trim().isEmpty()) {
            throw new ValidationException("Decision is required (must be 'Approve' or 'Reject').");
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new ValidationException("Decision reason is required.");
        }

        ClaimStatus previousStatus = claim.getStatus();

        if (request.isApprove()) {
            if (request.assessedAmount() == null || request.assessedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Assessed amount is required and must be greater than zero for approval.");
            }
            ClaimStateMachine.requireTransition(previousStatus, ClaimStatus.PAYMENT_PROCESSING);
            claim.setStatus(ClaimStatus.PAYMENT_PROCESSING);
            claim.setAssessedAmount(request.assessedAmount());
            claim.setReason(request.reason());
            claim = claimRepository.saveAndFlush(claim);

            claimHistoryRepository.save(new ClaimHistory(
                    claim.getId(), previousStatus, ClaimStatus.PAYMENT_PROCESSING, currentUser.getUserId(),
                    "Claim approved for amount " + request.assessedAmount() + ": " + request.reason()));

            emitNotification(NotificationEvent.CLAIM_APPROVED, claim.getCustomerId(), Map.of(
                    "claimNo", claim.getClaimNo(),
                    "assessedAmount", request.assessedAmount().toPlainString(),
                    "reason", request.reason()
            ));

            return new ClaimResponse(
                    claim.getId(), claim.getClaimNo(), claim.getPolicyId(), claim.getStatus(),
                    claim.getClaimAmount(), claim.getAssessedAmount(), claim.isDuplicateFlag(),
                    "Claim approved and moved to Payment Processing"
            );
        } else if ("Reject".equalsIgnoreCase(request.decision())) {
            ClaimStateMachine.requireTransition(previousStatus, ClaimStatus.REJECTED);
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setReason(request.reason());
            claim = claimRepository.saveAndFlush(claim);

            claimHistoryRepository.save(new ClaimHistory(
                    claim.getId(), previousStatus, ClaimStatus.REJECTED, currentUser.getUserId(),
                    "Claim rejected: " + request.reason()));

            emitNotification(NotificationEvent.CLAIM_REJECTED, claim.getCustomerId(), Map.of(
                    "claimNo", claim.getClaimNo(),
                    "reason", request.reason()
            ));

            return new ClaimResponse(
                    claim.getId(), claim.getClaimNo(), claim.getPolicyId(), claim.getStatus(),
                    claim.getClaimAmount(), claim.getAssessedAmount(), claim.isDuplicateFlag(),
                    "Claim rejected"
            );
        } else {
            throw new ValidationException("Invalid decision value '" + request.decision() + "'. Must be 'Approve' or 'Reject'.");
        }
    }

    @Override
    public int closeStale() {
        // Daily scheduler job closing unresponded document verification claims >= 15 days
        // Handled by background scheduler or on-demand
        return 0;
    }

    @Override
    public ClaimResponse payout(Long claimId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        RoleSecurity.assertInsurerAccess(currentUser, claim.getInsurerId());

        ClaimStateMachine.requireTransition(claim.getStatus(), ClaimStatus.SETTLED);
        ClaimStatus prev = claim.getStatus();
        claim.setStatus(ClaimStatus.SETTLED);
        claim.setSettledAmount(claim.getAssessedAmount());
        claim = claimRepository.saveAndFlush(claim);

        claimHistoryRepository.save(new ClaimHistory(
                claim.getId(), prev, ClaimStatus.SETTLED, currentUser.getUserId(), "Payout settled"));

        emitNotification(NotificationEvent.CLAIM_SETTLED, claim.getCustomerId(), Map.of(
                "claimNo", claim.getClaimNo(),
                "settledAmount", claim.getSettledAmount() != null ? claim.getSettledAmount().toPlainString() : ""
        ));

        return new ClaimResponse(
                claim.getId(), claim.getClaimNo(), claim.getPolicyId(), claim.getStatus(),
                claim.getClaimAmount(), claim.getAssessedAmount(), claim.isDuplicateFlag(),
                "Claim settled"
        );
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private void assertAccess(CurrentUser currentUser, Claim claim) {
        if (currentUser.isCustomer()) {
            if (!currentUser.getUserId().equals(claim.getCustomerId())) {
                throw new AccessDeniedException("Access denied: customer may only access their own claims.");
            }
            return;
        }
        if (currentUser.isClaimsOfficer() || currentUser.isStaff()) {
            RoleSecurity.assertInsurerAccess(currentUser, claim.getInsurerId());
            return;
        }
        if (currentUser.isSystemAdmin()) {
            return;
        }
        throw new AccessDeniedException("Access denied: unauthorized role.");
    }

    private String generateClaimNo(int year) {
        long seq = claimSeq.incrementAndGet();
        String candidate = String.format("CLM-%d-%06d", year, seq);
        while (claimRepository.findByClaimNo(candidate).isPresent()) {
            seq = claimSeq.incrementAndGet();
            candidate = String.format("CLM-%d-%06d", year, seq);
        }
        return candidate;
    }

    private void emitNotification(NotificationEvent event, Long recipientUserId, Map<String, String> params) {
        if (notifyProvider != null && recipientUserId != null) {
            INotify notify = notifyProvider.getIfAvailable();
            if (notify != null) {
                try {
                    notify.send(event, recipientUserId, params);
                } catch (Exception ex) {
                    log.warn("Notification emission for event {} to user {} failed: {}", event, recipientUserId, ex.getMessage());
                }
            }
        }
    }

    private ClaimView toClaimView(Claim claim) {
        return new ClaimView(
                claim.getId(),
                claim.getClaimNo(),
                claim.getPolicyId(),
                claim.getPolicyNo(),
                claim.getCustomerId(),
                claim.getInsurerId(),
                claim.getIncidentDate(),
                claim.getClaimAmount(),
                claim.getAssessedAmount(),
                claim.getStatus(),
                claim.getDescription(),
                claim.getReason(),
                claim.isDuplicateFlag(),
                new ArrayList<>(claim.getDocumentIds()),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }

    private PolicyInfo getPolicyInfo(Long policyId) {
        if (policyId == null) {
            throw new ResourceNotFoundException("Policy", null);
        }
        IPolicyQuery policyQuery = policyQueryProvider != null ? policyQueryProvider.getIfAvailable() : null;
        if (policyQuery != null) {
            try {
                PolicyView pv = policyQuery.getPolicy(policyId);
                return new PolicyInfo(pv.id(), pv.policyNo(), pv.customerId(), pv.insurerId(), pv.startDate(), pv.endDate(), pv.status());
            } catch (ResourceNotFoundException rnfe) {
                // Secondary fallback to policy repository if present
                PolicyRepository policyRepo = policyRepositoryProvider != null ? policyRepositoryProvider.getIfAvailable() : null;
                if (policyRepo != null) {
                    return policyRepo.findById(policyId)
                            .map(p -> new PolicyInfo(p.getId(), p.getPolicyNo(), p.getCustomerId(), p.getInsurerId(), p.getStartDate(), p.getEndDate(), p.getStatus()))
                            .orElseThrow(() -> rnfe);
                }
                throw rnfe;
            }
        }
        PolicyRepository policyRepo = policyRepositoryProvider != null ? policyRepositoryProvider.getIfAvailable() : null;
        if (policyRepo != null) {
            return policyRepo.findById(policyId)
                    .map(p -> new PolicyInfo(p.getId(), p.getPolicyNo(), p.getCustomerId(), p.getInsurerId(), p.getStartDate(), p.getEndDate(), p.getStatus()))
                    .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));
        }
        throw new ResourceNotFoundException("Policy", policyId);
    }

    private record PolicyInfo(Long id, String policyNo, Long customerId, Long insurerId, LocalDate startDate, LocalDate endDate, PolicyStatus status) {
    }

    // Lightweight internal ObjectProvider wrapper for testing
    private static class SimpleObjectProvider<T> implements ObjectProvider<T> {
        private final T value;

        SimpleObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
