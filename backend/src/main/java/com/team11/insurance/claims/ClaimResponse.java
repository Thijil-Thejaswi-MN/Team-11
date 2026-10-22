package com.team11.insurance.claims;

import java.math.BigDecimal;

/**
 * Authoritative Claim action response (SAD Table 3.4b, §4.3.5, P4 Contract §5.5).
 */
public record ClaimResponse(
        Long claimId,
        String claimNo,              // e.g. "CLM-2026-000101"
        Long policyId,
        ClaimStatus status,
        BigDecimal claimedAmount,
        BigDecimal assessedAmount,
        boolean duplicateFlag,
        String message
) {
}
