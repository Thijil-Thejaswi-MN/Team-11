package com.team11.insurance.plans;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.team11.insurance.common.exception.ResourceNotFoundException;

/**
 * Service tests for {@link PlanQueryService} and {@link IPlanQuery}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PlanQueryService.class)
class PlanQueryServiceTest {

    @Autowired
    private IPlanQuery planQuery;

    @Autowired
    private PlanRepository planRepository;

    private Plan motorPlan;
    private Plan healthPlanWithoutUw;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();

        motorPlan = new Plan(
                "PLAN-A-M1",
                1L,
                "MOTOR_FOUR_WHEELER",
                "SafeShield Comprehensive Car",
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"ageLoadingMinYears\":6,\"ageLoadingMaxYears\":10,\"ageLoadingPct\":0.05,\"addOns\":{\"ZERO_DEP\":1080.00}}",
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15,\"rejectIfActivePolicy\":true}",
                "[\"ZERO_DEP\"]",
                true
        );

        healthPlanWithoutUw = new Plan(
                "PLAN-A-H3",
                1L,
                "HEALTH",
                "SafeShield Health Unconfigured",
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                12,
                new BigDecimal("7500.00"),
                "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00}]}",
                null, // Explicitly no underwriting rules
                "[]",
                true
        );

        planRepository.saveAll(List.of(motorPlan, healthPlanWithoutUw));
    }

    @Test
    void getPlanReturnsExistingPlan() {
        Plan found = planQuery.getPlan(motorPlan.getId());
        assertNotNull(found);
        assertEquals("PLAN-A-M1", found.getCode());
        assertEquals("SafeShield Comprehensive Car", found.getName());
    }

    @Test
    void getPlanThrowsNotFoundForUnknownId() {
        assertThrows(ResourceNotFoundException.class, () -> planQuery.getPlan(99999L));
    }

    @Test
    void getRatingRulesExtractsRulesAndType() {
        RatingRules rules = planQuery.getRatingRules(motorPlan.getId());
        assertNotNull(rules);
        assertEquals("R-M", rules.getRuleType());
        assertEquals("PLAN-A-M1", rules.getPlanCode());
        assertTrue(rules.hasUnderwritingRules());
        assertTrue(rules.getPremiumRulesJson().contains("ownDamageBasePct"));
    }

    @Test
    void getRatingRulesHandlesMissingUnderwritingRulesSafely() {
        RatingRules rules = planQuery.getRatingRules(healthPlanWithoutUw.getId());
        assertNotNull(rules);
        assertEquals("R-H", rules.getRuleType());
        assertFalse(rules.hasUnderwritingRules());
        assertNull(rules.getUnderwritingRulesJson());
    }

    @Test
    void getActivePlansAndFilterByType() {
        List<Plan> active = planQuery.getActivePlans();
        assertEquals(2, active.size());

        List<Plan> motorPlans = planQuery.getPlansByType("MOTOR_FOUR_WHEELER");
        assertEquals(1, motorPlans.size());
        assertEquals("PLAN-A-M1", motorPlans.get(0).getCode());

        List<Plan> insurerPlans = planQuery.getPlansByInsurer(1L);
        assertEquals(2, insurerPlans.size());
    }

    @Test
    void existsByIdWorksCorrectly() {
        assertTrue(planQuery.existsById(motorPlan.getId()));
        assertFalse(planQuery.existsById(88888L));
    }
}
