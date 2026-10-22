package com.team11.insurance.claims;

import java.util.List;

/**
 * Claims officer document verification request (SAD Table 3.4b, §4.3.5, P4 Contract §5.5).
 */
public record ClaimVerificationRequest(
        boolean documentsComplete,
        List<String> missing,
        String notes
) {
}
