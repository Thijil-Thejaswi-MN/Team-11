package com.team11.insurance.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.MotorRules;

/**
 * Unit test suite for motor underwriting boundary rules (TC-UW-01, INS-F-010).
 * <p>
 * Verifies exact boundary values:
 * <ul>
 *   <li>Age 15 &rarr; Approve (boundary: &le; 15 is not older than 15)</li>
 *   <li>Age 16 &rarr; Refer</li>
 *   <li>Age 20 &rarr; Refer (boundary: &le; 20 is not older than 20)</li>
 *   <li>Age 21 &rarr; Reject</li>
 *   <li>Active policy exists &rarr; Reject (overrides Approve and Refer)</li>
 * </ul>
 */
class MotorUnderwritingRuleTest {

    private IPolicyQuery policyQuery;
    private MotorUnderwritingEvaluator evaluator;
    private final Clock clock2026 = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
    private final MotorRules standardRules = new MotorRules(20, 15, true);

    @BeforeEach
    void setUp() {
        policyQuery = mock(IPolicyQuery.class);
        evaluator = new MotorUnderwritingEvaluator(policyQuery, new ObjectMapper());
    }

    @ParameterizedTest(name = "regYear {0} -> age {1} should result in outcome {2}")
    @CsvSource({
            "2018,  8, APPROVE",
            "2011, 15, APPROVE",
            "2010, 16, REFER",
            "2006, 20, REFER",
            "2005, 21, REJECT",
            "2004, 22, REJECT"
    })
    @DisplayName("TC-UW-01: Vehicle age boundary evaluation without active policy")
    void vehicleAgeBoundaries(int regYear, int expectedAge, UnderwritingOutcome expectedOutcome) {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":" + regYear + ",\"vehicleType\":\"4W\"}");

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(false);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(expectedOutcome);
        assertThat(result.getReason()).isNotEmpty();
    }

    @Test
    @DisplayName("Active policy on vehicle overrides age 15 and forces REJECT")
    void activePolicyOverridesAge15Approve() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":2011,\"vehicleType\":\"4W\"}"); // Age 15

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Active policy on vehicle overrides age 16 and forces REJECT")
    void activePolicyOverridesAge16Refer() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":2010,\"vehicleType\":\"4W\"}"); // Age 16

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Active policy on vehicle with age 21 results in REJECT")
    void activePolicyWithAge21Reject() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":2005,\"vehicleType\":\"4W\"}"); // Age 21

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Direct vehicleAge field in detailsJson is supported")
    void supportsDirectVehicleAgeField() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB9999");
        application.setDetailsJson("{\"vehicleAge\":15}");

        when(policyQuery.existsActivePolicyForVehicle(anyString())).thenReturn(false);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.APPROVE);
    }

    @Test
    @DisplayName("Vehicle reg number parsed from detailsJson when absent from entity column")
    void extractsVehicleRegNoFromDetailsJson() {
        Application application = new Application();
        application.setDetailsJson("{\"vehicleRegNo\":\"KA05XY5555\",\"regYear\":2021}");

        when(policyQuery.existsActivePolicyForVehicle("KA05XY5555")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).contains("KA05XY5555");
    }

    @Test
    @DisplayName("Missing or invalid rule config returns REFER with missing configuration reason")
    void missingRuleConfigYieldsRefer() {
        Application application = new Application();
        application.setDetailsJson("{\"regYear\":2020}");

        UnderwritingResult result1 = evaluator.evaluate(application, null, clock2026);
        assertThat(result1.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result1.getReason()).isEqualTo("Missing rule configuration");

        UnderwritingResult result2 = evaluator.evaluate(application, new MotorRules(null, 15, true), clock2026);
        assertThat(result2.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result2.getReason()).isEqualTo("Missing rule configuration");
    }

    @Test
    @DisplayName("Active policy exists even when vehicle age is completely missing results in REJECT (not downgraded to REFER)")
    void activePolicyRejectionEnforcedWhenVehicleAgeMissing() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{}"); // Missing regYear and vehicleAge

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, standardRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Plan configuration cannot disable mandatory active policy rejection")
    void planConfigurationCannotDisableActivePolicyRejection() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":2021}");

        // Plan rule with rejectIfActivePolicy explicitly set to false
        MotorRules permissiveRules = new MotorRules(20, 15, false);
        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(true);

        UnderwritingResult result = evaluator.evaluate(application, permissiveRules, clock2026);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Future registration year throws ValidationException and never produces APPROVE")
    void futureRegistrationYearThrowsValidationException() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"regYear\":2030}"); // Future year relative to clock 2026

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluator.evaluate(application, standardRules, clock2026))
                .isInstanceOf(com.team11.insurance.common.exception.ValidationException.class)
                .hasMessageContaining("registration year cannot be in the future");
    }

    @Test
    @DisplayName("Explicitly negative vehicle age throws ValidationException and never produces APPROVE")
    void negativeVehicleAgeThrowsValidationException() {
        Application application = new Application();
        application.setVehicleRegNo("KA01AB1234");
        application.setDetailsJson("{\"vehicleAge\":-5}");

        when(policyQuery.existsActivePolicyForVehicle("KA01AB1234")).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluator.evaluate(application, standardRules, clock2026))
                .isInstanceOf(com.team11.insurance.common.exception.ValidationException.class);
    }
}
