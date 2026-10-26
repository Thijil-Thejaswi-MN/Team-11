package com.team11.insurance.common;

import com.team11.insurance.common.logging.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    @DisplayName("Masks PAN according to SAD Section 4.4 example ABCDE****F")
    void testMaskPan() {
        assertThat(SensitiveDataMasker.maskPan("ABCDE1234F")).isEqualTo("ABCDE****F");
        assertThat(SensitiveDataMasker.maskPan("BKZPK9876Q")).isEqualTo("BKZPK****Q");
        assertThat(SensitiveDataMasker.maskPan("SHORT")).isEqualTo("******");
        assertThat(SensitiveDataMasker.maskPan(null)).isEqualTo("******");
    }

    @Test
    @DisplayName("Masks 12-digit Aadhaar number preserving last 4 digits")
    void testMaskAadhaar() {
        assertThat(SensitiveDataMasker.maskAadhaar("123456789012")).isEqualTo("********9012");
        assertThat(SensitiveDataMasker.maskAadhaar("998877665544")).isEqualTo("********5544");
        assertThat(SensitiveDataMasker.maskAadhaar("1234 5678 9012")).isEqualTo("********9012");
        assertThat(SensitiveDataMasker.maskAadhaar("1234-5678-9012")).isEqualTo("********9012");
        assertThat(SensitiveDataMasker.maskAadhaar("123")).isEqualTo("********");
        assertThat(SensitiveDataMasker.maskAadhaar(null)).isEqualTo("********");
    }

    @Test
    @DisplayName("Masks email address protecting user privacy")
    void testMaskEmail() {
        assertThat(SensitiveDataMasker.maskEmail("customer@example.com")).isEqualTo("c***r@example.com");
        assertThat(SensitiveDataMasker.maskEmail("ab@domain.com")).isEqualTo("*@domain.com");
        assertThat(SensitiveDataMasker.maskEmail("invalid")).isEqualTo("***");
        assertThat(SensitiveDataMasker.maskEmail(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("Masks telephone numbers protecting mobile identity")
    void testMaskPhone() {
        assertThat(SensitiveDataMasker.maskPhone("+91 9876543210")).isEqualTo("+91 ******3210");
        assertThat(SensitiveDataMasker.maskPhone("+91-9876543210")).isEqualTo("+91 ******3210");
        assertThat(SensitiveDataMasker.maskPhone("9876543210")).isEqualTo("******3210");
        assertThat(SensitiveDataMasker.maskPhone("123")).isEqualTo("******");
        assertThat(SensitiveDataMasker.maskPhone(null)).isEqualTo("******");
    }

    @Test
    @DisplayName("Masks credit and debit card numbers to PCI-DSS standard preserving last 4 digits")
    void testMaskCard() {
        assertThat(SensitiveDataMasker.maskCard("1234567890123456")).isEqualTo("************3456");
        assertThat(SensitiveDataMasker.maskCard("4111 2222 3333 4444")).isEqualTo("************4444");
        assertThat(SensitiveDataMasker.maskCard("4111-2222-3333-4444")).isEqualTo("************4444");
        assertThat(SensitiveDataMasker.maskCard("123")).isEqualTo("************");
        assertThat(SensitiveDataMasker.maskCard(null)).isEqualTo("************");
    }

    @Test
    @DisplayName("Masks CVV security codes completely")
    void testMaskCvv() {
        assertThat(SensitiveDataMasker.maskCvv("123")).isEqualTo("***");
        assertThat(SensitiveDataMasker.maskCvv("1234")).isEqualTo("****");
        assertThat(SensitiveDataMasker.maskCvv(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("Masks authentication tokens and secrets")
    void testMaskToken() {
        assertThat(SensitiveDataMasker.maskToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0In0")).isEqualTo("eyJh...***");
        assertThat(SensitiveDataMasker.maskToken("secret12")).isEqualTo("***");
        assertThat(SensitiveDataMasker.maskToken(null)).isEqualTo("***");
    }

    @Test
    @DisplayName("Replaces raw passwords with non-reversible [PROTECTED] mask")
    void testMaskPassword() {
        assertThat(SensitiveDataMasker.maskPassword("SuperSecretP@ssword123")).isEqualTo("[PROTECTED]");
        assertThat(SensitiveDataMasker.maskPassword(null)).isEqualTo("[PROTECTED]");
    }

    @Test
    @DisplayName("Generic pattern-based log scrubber sanitizes PAN, Aadhaar, Card, Password, Token, and CVV")
    void testMaskGeneric() {
        String logLine = "Error processing payload: {\"password\":\"MySecretPass1!\",\"accessToken\":\"eyJhbGci...\",\"pan\":\"ABCDE1234F\",\"aadhaar\":\"1234 5678 9012\",\"card\":\"4111-2222-3333-4444\",cvv: 123}";
        String sanitized = SensitiveDataMasker.maskGeneric(logLine);

        assertThat(sanitized).doesNotContain("MySecretPass1!");
        assertThat(sanitized).contains("\"password\": \"[PROTECTED]\"");
        assertThat(sanitized).doesNotContain("ABCDE1234F");
        assertThat(sanitized).contains("ABCDE****F");
        assertThat(sanitized).doesNotContain("1234 5678 9012");
        assertThat(sanitized).contains("********9012");
        assertThat(sanitized).doesNotContain("4111-2222-3333-4444");
        assertThat(sanitized).contains("************4444");
        assertThat(sanitized).contains("cvv: \"***\"");
    }

    @Test
    @DisplayName("User entity toString() omits passwordHash and masks email and phone")
    void testUserToStringMasksSensitiveFields() {
        com.team11.insurance.auth.entity.User user = new com.team11.insurance.auth.entity.User(
                "Jane Doe",
                "janedoe@example.com",
                "+91 9876543210",
                "$2a$12$e8k...secretHash",
                com.team11.insurance.auth.UserRole.CUSTOMER,
                null
        );

        String userString = user.toString();
        assertThat(userString).doesNotContain("secretHash");
        assertThat(userString).doesNotContain("$2a$12");
        assertThat(userString).doesNotContain("janedoe@example.com");
        assertThat(userString).contains("j***e@example.com");
        assertThat(userString).doesNotContain("9876543210");
        assertThat(userString).contains("+91 ******3210");
    }

    @Test
    @DisplayName("LoginRequest and RegisterRequest toString() protect passwords and mask contact PII")
    void testAuthRequestsToStringProtectPasswords() {
        com.team11.insurance.auth.dto.LoginRequest login = new com.team11.insurance.auth.dto.LoginRequest(
                "customer@insurance.com",
                "SuperSecretPassword!1"
        );
        String loginStr = login.toString();
        assertThat(loginStr).doesNotContain("SuperSecretPassword!1");
        assertThat(loginStr).contains("[PROTECTED]");
        assertThat(loginStr).doesNotContain("customer@insurance.com");
        assertThat(loginStr).contains("c***r@insurance.com");

        com.team11.insurance.auth.dto.RegisterRequest register = new com.team11.insurance.auth.dto.RegisterRequest(
                "Alice Smith",
                "alice@example.com",
                "9876543210",
                "VerySecurePassword@123"
        );
        String regStr = register.toString();
        assertThat(regStr).doesNotContain("VerySecurePassword@123");
        assertThat(regStr).contains("[PROTECTED]");
        assertThat(regStr).doesNotContain("alice@example.com");
        assertThat(regStr).contains("a***e@example.com");
        assertThat(regStr).doesNotContain("9876543210");
        assertThat(regStr).contains("******3210");
    }
}
