package com.team11.insurance.claims;

/**
 * Authoritative Claim Lifecycle States (SAD §4.1.3, SRS §4.7.1, P4-03 Contract §5.2).
 */
public enum ClaimStatus {
    SUBMITTED("Submitted"),
    DOCUMENT_VERIFICATION("Document Verification"),
    ASSESSMENT("Assessment"),
    APPROVED("Approved"),
    PAYMENT_PROCESSING("Payment Processing"),
    SETTLED("Settled"),
    REJECTED("Rejected"),
    CLOSED_NO_RESPONSE("Closed - No Response");

    private final String displayName;

    ClaimStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
