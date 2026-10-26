package com.team11.insurance.policies;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyPdfGeneratorTest {

    private final PolicyPdfGenerator pdfGenerator = new PolicyPdfGenerator();

    @Test
    @DisplayName("Generates valid OpenPDF document with %PDF- header")
    void testGeneratePolicyPdf_ValidPdfHeader() {
        Policy policy = new Policy();
        policy.setPolicyNo("POL-2026-000101");
        policy.setApplicationId(101L);
        policy.setCustomerId(1L);
        policy.setInsurerId(1L);
        policy.setPlanId(1L);
        policy.setVehicleRegNo("KA-01-AB-1234");
        policy.setPremiumPaid(new BigDecimal("9480.00"));
        policy.setStartDate(LocalDate.of(2026, 10, 23));
        policy.setEndDate(LocalDate.of(2027, 10, 22));
        policy.setStatus(PolicyStatus.ACTIVE);

        byte[] pdfBytes = pdfGenerator.generatePolicyPdf(policy, "Comprehensive Motor Plan", "Jane Doe");

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertThat(header).startsWith("%PDF-");
    }

    @Test
    @DisplayName("Generates PDF schedule with null optional fields safely")
    void testGeneratePolicyPdf_NullOptionalFields() {
        Policy policy = new Policy();
        policy.setPolicyNo("POL-2026-000102");
        policy.setCustomerId(2L);
        policy.setInsurerId(1L);
        policy.setPlanId(3L);
        policy.setPremiumPaid(new BigDecimal("14500.00"));
        policy.setStartDate(LocalDate.of(2026, 10, 23));
        policy.setEndDate(LocalDate.of(2027, 10, 22));
        policy.setStatus(PolicyStatus.ACTIVE);

        byte[] pdfBytes = pdfGenerator.generatePolicyPdf(policy, null, null);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertThat(header).startsWith("%PDF-");
    }
}
