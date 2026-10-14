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
}
