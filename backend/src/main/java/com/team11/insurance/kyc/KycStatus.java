package com.team11.insurance.kyc;

/**
 * KYC verification status values aligned with SAD §4.3.1 and API Contract 02 §3.
 */
public enum KycStatus {
    PENDING("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    NOT_SUBMITTED("Not Submitted");

    private final String displayValue;

    KycStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static KycStatus fromString(String val) {
        if (val == null) return NOT_SUBMITTED;
        for (KycStatus s : values()) {
            if (s.name().equalsIgnoreCase(val) || s.displayValue.equalsIgnoreCase(val)) {
                return s;
            }
        }
        return NOT_SUBMITTED;
    }
}
