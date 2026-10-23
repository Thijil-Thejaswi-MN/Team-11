package com.team11.insurance.claims;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.team11.insurance.auth.CurrentUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Development and test stub for {@link IClaimService} (Work Item: P4-06).
 * <p>
 * Implements the agreed {@link #hasPaidClaim(Long)} signature.
 * By default, returns {@code false} as specified for the P4-06 stub baseline.
 * Provides test helpers to inject synthetic values per policy ID for cancellation refund verification.
 * <p>
 * <b>Important Domain Boundary Notice:</b>
 * The real Claim entity, JPA repository, state machine, and full workflow are owned exclusively by Person 3 (P3-02/P3-05).
 * This stub does NOT introduce or alter any competing domain models.
 * <p>
 * Strictly active under {@code dev} and {@code test} profiles.
 */
@Service
@Profile({"dev", "test"})
public class ClaimServiceStub implements IClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimServiceStub.class);

    private final Map<Long, Boolean> syntheticPaidClaims = new ConcurrentHashMap<>();

    @Override
    public boolean hasPaidClaim(Long policyId) {
        log.info("ClaimServiceStub: Checking hasPaidClaim for policyId={}", policyId);
        if (policyId == null) {
            return false;
        }
        return syntheticPaidClaims.getOrDefault(policyId, false);
    }

    @Override
    public ClaimResponse file(ClaimSubmissionRequest request, CurrentUser currentUser) {
        log.info("ClaimServiceStub: file called for request={}", request);
        throw new UnsupportedOperationException("ClaimServiceStub: full claims workflow is provided by ClaimService (P4-09)");
    }

    @Override
    public ClaimView get(Long claimId, CurrentUser currentUser) {
        log.info("ClaimServiceStub: get called for claimId={}", claimId);
        throw new UnsupportedOperationException("ClaimServiceStub: full claims workflow is provided by ClaimService (P4-09)");
    }

    @Override
    public java.util.List<ClaimView> list(Long policyId, CurrentUser currentUser) {
        log.info("ClaimServiceStub: list called for policyId={}", policyId);
        return java.util.List.of();
    }

    @Override
    public java.util.List<ClaimView> listForInsurer(Long insurerId, ClaimStatus status, CurrentUser currentUser) {
        log.info("ClaimServiceStub: listForInsurer called for insurerId={}, status={}", insurerId, status);
        return java.util.List.of();
    }

    @Override
    public ClaimResponse verify(Long claimId, ClaimVerificationRequest request, CurrentUser currentUser) {
        log.info("ClaimServiceStub: verify called for claimId={}", claimId);
        throw new UnsupportedOperationException("ClaimServiceStub: full claims workflow is provided by ClaimService (P4-09)");
    }

    @Override
    public ClaimResponse decide(Long claimId, ClaimDecisionRequest request, CurrentUser currentUser) {
        log.info("ClaimServiceStub: decide called for claimId={}", claimId);
        throw new UnsupportedOperationException("ClaimServiceStub: full claims workflow is provided by ClaimService (P4-09)");
    }

    @Override
    public int closeStale() {
        log.info("ClaimServiceStub: closeStale called");
        return 0;
    }

    @Override
    public ClaimResponse payout(Long claimId, CurrentUser currentUser) {
        log.info("ClaimServiceStub: payout called for claimId={}", claimId);
        throw new UnsupportedOperationException("ClaimServiceStub: full claims workflow is provided by ClaimService (P4-09)");
    }

    // =========================================================================
    // Test & Diagnostic Helpers
    // =========================================================================

    public void setHasPaidClaim(Long policyId, boolean hasPaidClaim) {
        if (policyId != null) {
            syntheticPaidClaims.put(policyId, hasPaidClaim);
            log.info("ClaimServiceStub: Set synthetic hasPaidClaim={} for policyId={}", hasPaidClaim, policyId);
        }
    }

    public void reset() {
        syntheticPaidClaims.clear();
        log.info("ClaimServiceStub: Reset synthetic claim overrides.");
    }
}
