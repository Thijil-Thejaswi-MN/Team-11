package com.team11.insurance.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to mask sensitive credentials, financial data, and customer PII before logging.
 * Mandated by SAD Section 4.4, 4.5, INS-SR-004, and STP TC-Sec-11.
 */
public final class SensitiveDataMasker {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b([A-Z]{5})[0-9]{4}([A-Z])\\b");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){12,18}\\d\\b");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b(\\d{4})[ -]?(\\d{4})[ -]?(\\d{4})\\b(?![ -]?\\d)");
    private static final Pattern PASSWORD_JSON_PATTERN = Pattern.compile("(?i)\"(password|newPassword|confirmPassword|secret|client_secret)\"\\s*:\\s*\"[^\"]*\"");
    private static final Pattern PASSWORD_TEXT_PATTERN = Pattern.compile("(?i)\\b(password|newPassword|confirmPassword|secret|client_secret)\\s*[:=]\\s*\"?([^\",\\s}]+)\"?");
    private static final Pattern TOKEN_JSON_PATTERN = Pattern.compile("(?i)\"(accessToken|refreshToken|token|jwt|apiKey|authorization)\"\\s*:\\s*\"[^\"]*\"");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)\\b(cvv|cvc|cvv2)\\s*[:=]\\s*\"?(\\d{3,4})\"?");

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
        if (aadhaar == null) {
            return "********";
        }
        String clean = aadhaar.replaceAll("[ -]", "");
        if (clean.length() < 4) {
            return "********";
        }
        return "********" + clean.substring(clean.length() - 4);
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

    /**
     * Masks telephone / mobile numbers, e.g. "+91 9876543210" -> "+91 ******3210" or "******3210"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "******";
        }
        String trimmed = phone.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "******";
        }
        String last4 = digits.substring(digits.length() - 4);
        if (trimmed.startsWith("+91")) {
            return "+91 ******" + last4;
        } else if (trimmed.startsWith("+")) {
            int firstSpace = trimmed.indexOf(' ');
            if (firstSpace > 0) {
                return trimmed.substring(0, firstSpace) + " ******" + last4;
            }
        }
        return "******" + last4;
    }

    /**
     * Masks credit/debit card numbers (PCI-DSS), preserving only last 4 digits, e.g. "************3456"
     */
    public static String maskCard(String cardNumber) {
        if (cardNumber == null) {
            return "************";
        }
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "************";
        }
        return "************" + digits.substring(digits.length() - 4);
    }

    /**
     * Masks CVV/CVC security codes, returning "***"
     */
    public static String maskCvv(String cvv) {
        if (cvv != null && cvv.trim().length() == 4) {
            return "****";
        }
        return "***";
    }

    /**
     * Masks auth tokens, API keys, and session secrets
     */
    public static String maskToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "***";
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return "***";
        }
        return trimmed.substring(0, 4) + "...***";
    }

    /**
     * Replaces raw passwords with non-reversible mask
     */
    public static String maskPassword(String password) {
        return "[PROTECTED]";
    }

    /**
     * Generic pattern-based log scrubber: sanitizes unstructured log messages,
     * JSON error dumps, and exception messages by replacing PANs, Aadhaars,
     * card numbers, passwords, tokens, and CVVs with masked values.
     */
    public static String maskGeneric(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // 1. Mask JSON password fields
        result = PASSWORD_JSON_PATTERN.matcher(result).replaceAll("\"$1\": \"[PROTECTED]\"");
        result = PASSWORD_TEXT_PATTERN.matcher(result).replaceAll("$1: \"[PROTECTED]\"");

        // 2. Mask JSON token fields
        result = TOKEN_JSON_PATTERN.matcher(result).replaceAll("\"$1\": \"[PROTECTED]\"");

        // 3. Mask CVV occurrences
        result = CVV_PATTERN.matcher(result).replaceAll("$1: \"***\"");

        // 4. Mask PAN occurrences
        Matcher panMatcher = PAN_PATTERN.matcher(result);
        StringBuffer panSb = new StringBuffer();
        while (panMatcher.find()) {
            panMatcher.appendReplacement(panSb, panMatcher.group(1) + "****" + panMatcher.group(2));
        }
        panMatcher.appendTail(panSb);
        result = panSb.toString();

        // 5. Mask credit card numbers (13-19 digits)
        Matcher cardMatcher = CARD_PATTERN.matcher(result);
        StringBuffer cardSb = new StringBuffer();
        while (cardMatcher.find()) {
            String fullMatch = cardMatcher.group(0);
            String clean = fullMatch.replaceAll("\\D", "");
            if (clean.length() >= 13 && clean.length() <= 19) {
                cardMatcher.appendReplacement(cardSb, "************" + clean.substring(clean.length() - 4));
            } else {
                cardMatcher.appendReplacement(cardSb, Matcher.quoteReplacement(fullMatch));
            }
        }
        cardMatcher.appendTail(cardSb);
        result = cardSb.toString();

        // 6. Mask Aadhaar occurrences (12 digits)
        Matcher aadhaarMatcher = AADHAAR_PATTERN.matcher(result);
        StringBuffer aadhaarSb = new StringBuffer();
        while (aadhaarMatcher.find()) {
            aadhaarMatcher.appendReplacement(aadhaarSb, "********" + aadhaarMatcher.group(3));
        }
        aadhaarMatcher.appendTail(aadhaarSb);
        result = aadhaarSb.toString();

        return result;
    }
}
