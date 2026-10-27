package com.team11.insurance.notifications;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.team11.insurance.notifications.template.NotificationPayload;
import com.team11.insurance.notifications.template.NotificationTemplateService;
import com.team11.insurance.notifications.template.NotificationValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("P4-08 Notification Template Service Tests (All 16 Canonical Events)")
class NotificationTemplateServiceTest {

    private NotificationTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new NotificationTemplateService();
    }

    @ParameterizedTest
    @EnumSource(NotificationEvent.class)
    @DisplayName("Every canonical event in SAD §4.1.7 has a registered template and renders valid payloads")
    void testAllSixteenCanonicalEventTemplatesExist(NotificationEvent event) {
        Map<String, String> params = buildCanonicalParams(event);

        NotificationPayload payload = templateService.render(event, params);

        assertThat(payload).isNotNull();
        assertThat(payload.subject()).as("Subject for " + event).isNotBlank();
        assertThat(payload.emailBody()).as("Email body for " + event).isNotBlank();
        assertThat(payload.smsBody()).as("SMS body for " + event).isNotBlank();
    }

    @Test
    @DisplayName("CLAIM_REJECTED fails validation when required 'reason' parameter is missing")
    void testClaimRejectedValidationFailure() {
        Map<String, String> invalidParams = Map.of("claimNo", "CLM-2026-001");

        assertThatThrownBy(() -> templateService.render(NotificationEvent.CLAIM_REJECTED, invalidParams))
                .isInstanceOf(NotificationValidationException.class)
                .hasMessageContaining("reason");
    }

    @Test
    @DisplayName("POLICY_ISSUED fails validation when required 'policyNo' parameter is missing")
    void testPolicyIssuedValidationFailure() {
        Map<String, String> invalidParams = Map.of("amount", "12500");

        assertThatThrownBy(() -> templateService.render(NotificationEvent.POLICY_ISSUED, invalidParams))
                .isInstanceOf(NotificationValidationException.class)
                .hasMessageContaining("policyNo");
    }

    @Test
    @DisplayName("PAYMENT_FAILED fails validation when required identifier is missing")
    void testPaymentFailedValidationFailure() {
        Map<String, String> invalidParams = Map.of("reason", "Card declined");

        assertThatThrownBy(() -> templateService.render(NotificationEvent.PAYMENT_FAILED, invalidParams))
                .isInstanceOf(NotificationValidationException.class)
                .hasMessageContaining("applicationId");
    }

    @Test
    @DisplayName("CANCELLATION_APPROVED renders refund amount appropriately")
    void testCancellationApprovedRendering() {
        Map<String, String> params = Map.of(
                "policyNo", "POL-9988",
                "refundAmount", "4500.00"
        );

        NotificationPayload payload = templateService.render(NotificationEvent.CANCELLATION_APPROVED, params);

        assertThat(payload.subject()).contains("POL-9988");
        assertThat(payload.emailBody()).contains("POL-9988").contains("4500.00");
        assertThat(payload.smsBody()).contains("POL-9988").contains("4500.00");
    }

    @Test
    @DisplayName("RENEWAL_REMINDER renders daysRemaining and expiryDate")
    void testRenewalReminderRendering() {
        Map<String, String> params = Map.of(
                "policyNo", "POL-7766",
                "daysRemaining", "15",
                "expiryDate", "2026-11-01"
        );

        NotificationPayload payload = templateService.render(NotificationEvent.RENEWAL_REMINDER, params);

        assertThat(payload.subject()).contains("POL-7766").contains("15 days");
        assertThat(payload.emailBody()).contains("POL-7766").contains("2026-11-01");
        assertThat(payload.smsBody()).contains("15 days").contains("POL-7766");
    }

    private Map<String, String> buildCanonicalParams(NotificationEvent event) {
        Map<String, String> params = new HashMap<>();
        params.put("applicationId", "APP-1001");
        params.put("policyNo", "POL-2026-001");
        params.put("claimNo", "CLM-2026-001");
        params.put("amount", "15000.00");
        params.put("refundAmount", "3000.00");
        params.put("payoutAmount", "12000.00");
        params.put("reason", "Standard underwriting assessment");
        params.put("daysRemaining", "30");
        params.put("expiryDate", "2026-12-31");
        params.put("documents", "Police report and repair estimate");
        return params;
    }
}
