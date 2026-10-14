package com.team11.insurance.common.logging;

/**
 * Utility to mask sensitive credentials and customer PII before logging.
 * Mandated by SAD Section 4.4 and 4.5.
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
        // Utility class
    }

    /**
     * Masks PAN (Permanent Account Number), e.g. "ABCDE1234F" -> "ABCDE****F"
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "******";
        }
        return pan.substring(0, 5) + "****" + pan.substring(pan.length() - 1);
    }

    /**
     * Masks 12-digit Aadhaar Number, e.g. "123456789012" -> "********9012"
     */
    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) {
            return "********";
        }
        return "********" + aadhaar.substring(aadhaar.length() - 4);
    }

    /**
     * Masks emails, e.g. "customer@example.com" -> "c***r@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        if (name.length() <= 2) {
            return "*@" + parts[1];
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + parts[1];
    }
}
