package com.team11.insurance.claims;

import java.math.BigDecimal;

/**
 * Claims officer assessment decision request (SAD Table 3.4b, §4.3.5, P1 Contract §6.2, P4 Contract §5.5).
 */
public record ClaimDecisionRequest(
        String decision,             // Required: "Approve" or "Reject"
        BigDecimal assessedAmount,   // Required if decision is "Approve"
        String reason                // Required validation per P1 contract
) {
    public boolean isApprove() {
        return "Approve".equalsIgnoreCase(decision);
    }
}
