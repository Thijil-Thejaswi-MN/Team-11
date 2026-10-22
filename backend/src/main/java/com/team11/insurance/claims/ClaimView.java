package com.team11.insurance.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Detailed Claim view with audit history attributes (SAD Table 3.4b, §4.3.5, P4 Contract §5.5).
 */
public record ClaimView(
        Long id,
        String claimNo,
        Long policyId,
        String policyNo,
        Long customerId,
        Long insurerId,
        LocalDate incidentDate,
        BigDecimal claimedAmount,
        BigDecimal assessedAmount,
        ClaimStatus status,
        String description,
        String reason,               // Decision reason or rejection reason
        boolean duplicateFlag,
        List<Long> documentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
