package com.team11.insurance.claims;

/**
 * Claims Workflow Interface (Component C7 - SAD Table 3.4b, Sections 4.1.3, 4.3.5 & ADR-008).
 * Consumed by: Quote & Purchase (hasPaidClaim), Policy Cancellation, Claims Workflow, Scheduler.
 */
public interface IClaimService {

    /**
     * Checks if any claim has been settled on a policy (SAD Table 3.4b, Decision D-09.5, P4-03 Section 5.1).
     * <p>
     * <b>Production Semantics:</b>
     * Returns {@code true} ONLY if a claim associated with {@code policyId} is in status {@code SETTLED}.
     * Claims in {@code APPROVED} or {@code PAYMENT_PROCESSING} that have not completed payout do not count as paid.
     * When a policy has settled claims, cancellation refund is calculated as Rs 0 (zero-refund rule).
     *
     * @param policyId the primary key of the policy
     * @return true if any claim associated with policyId is SETTLED; otherwise false
     */
    boolean hasPaidClaim(Long policyId);
}
