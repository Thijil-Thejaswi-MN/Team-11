package com.team11.insurance.claims;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Customer claim submission request (SAD Table 3.4b, §4.3.5, P4 Contract §5.5).
 */
public record ClaimSubmissionRequest(
        Long policyId,
        LocalDate incidentDate,
        BigDecimal claimedAmount,
        String description,
        List<Long> documentIds
) {
}
