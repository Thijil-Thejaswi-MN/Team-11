package com.team11.insurance.claims;

import java.util.List;

import com.team11.insurance.auth.CurrentUser;

/**
 * Claims Workflow Interface (Component C7 - SAD Table 3.4b, §§4.1.3, 4.3.5 & ADR-008).
 * Consumed by: REST API Layer, Quote & Purchase (hasPaidClaim), Scheduler (closeStale).
 */
public interface IClaimService {

    /**
     * Checks if any claim has been settled on a policy.
     * Agreed canonical rule (Decision D-09.5, confirmed by team review):
     * Returns true ONLY if a claim on policyId has status SETTLED.
     * (An Approved or Payment Processing claim that has not completed payout does NOT count as paid).
     *
     * @param policyId the primary key of the policy
     * @return true if any claim associated with policyId is SETTLED; otherwise false
     */
    boolean hasPaidClaim(Long policyId);

    /**
     * Files a claim against an active policy within 30 days of incident (SAD Table 3.4b 'file').
     * Enters Submitted, runs duplicate detection, and advances to Document Verification.
     */
    ClaimResponse file(ClaimSubmissionRequest request, CurrentUser currentUser);

    /**
     * Retrieves claim details and status history (SAD Table 3.4b 'get').
     */
    ClaimView get(Long claimId, CurrentUser currentUser);

    /**
     * Lists claims belonging to a specific policy (SAD Table 3.4b 'list').
     */
    List<ClaimView> list(Long policyId, CurrentUser currentUser);

    /**
     * Lists claims for insurer staff scoped to currentUser.insurerId with optional status filter.
     */
    List<ClaimView> listForInsurer(Long insurerId, ClaimStatus status, CurrentUser currentUser);

    /**
     * Claims Officer verifies documents (SAD Table 3.4b 'verify').
     * If documentsComplete == true, transitions to Assessment.
     * If documentsComplete == false, requests documents and emits CLAIM_DOCUMENTS_REQUESTED (remains in Document Verification).
     */
    ClaimResponse verify(Long claimId, ClaimVerificationRequest request, CurrentUser currentUser);

    /**
     * Claims Officer approves or rejects claim after assessment (SAD Table 3.4b 'decide').
     * Aligned with P1 Contract §6.2: decision ("Approve"|"Reject"), assessedAmount, reason.
     * Approval transitions directly to Payment Processing (SAD §4.1.3, §4.3.5).
     */
    ClaimResponse decide(Long claimId, ClaimDecisionRequest request, CurrentUser currentUser);

    /**
     * Daily scheduler job (SAD Table 3.4b 'closeStale', TC-Clm-06):
     * Closes claims in Document Verification with no response to document requests after 15 days.
     * Transitions to Closed - No Response and emits CLAIM_CLOSED.
     *
     * @return count of claims closed
     */
    int closeStale();

    /**
     * Disburses approved payout through IPaymentService.
     * On payout failure, claim remains in Payment Processing and is retried.
     */
    ClaimResponse payout(Long claimId, CurrentUser currentUser);

    // =========================================================================
    // Compatibility Adapter Aliases (Proposed helpers for controller convenience)
    // =========================================================================
    default ClaimResponse fileClaim(ClaimSubmissionRequest r, CurrentUser u) { return file(r, u); }
    default ClaimView getClaim(Long id, CurrentUser u) { return get(id, u); }
    default ClaimResponse verifyDocuments(Long id, ClaimVerificationRequest r, CurrentUser u) { return verify(id, r, u); }
    default ClaimResponse recordDecision(Long id, ClaimDecisionRequest r, CurrentUser u) { return decide(id, r, u); }
    default int autoCloseStaleClaims() { return closeStale(); }
}
