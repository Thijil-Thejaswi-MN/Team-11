package com.team11.insurance.plans;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for {@link PlanDataSeeder} verifying canonical STP Section 6 plans and rules.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanDataSeederTest {

    @Autowired
    private PlanRepository planRepository;

    private PlanDataSeeder seeder;

    @BeforeEach
    void setUp() {
        planRepository.deleteAll();
        seeder = new PlanDataSeeder(planRepository);
    }

    @Test
    void seedsAllSevenStpSection6Plans() {
        seeder.run();

        assertEquals(7, planRepository.count());

        // Verify PLAN-A-M1 (Motor 4-wheeler, Insurer 1)
        Optional<Plan> am1 = planRepository.findByCode("PLAN-A-M1");
        assertTrue(am1.isPresent());
        assertEquals("MOTOR_FOUR_WHEELER", am1.get().getType());
        assertEquals(1L, am1.get().getInsurerId());
        assertEquals(new BigDecimal("500000.00"), am1.get().getCoverage());
        assertEquals(new BigDecimal("2000.00"), am1.get().getDeductible());
        assertTrue(am1.get().getPremiumRulesJson().contains("ownDamageBasePct"));
        assertTrue(am1.get().getPremiumRulesJson().contains("ZERO_DEP"));

        // Verify PLAN-A-H1 (Health 5L, Insurer 1, 8500 premium)
        Optional<Plan> ah1 = planRepository.findByCode("PLAN-A-H1");
        assertTrue(ah1.isPresent());
        assertEquals("HEALTH", ah1.get().getType());
        assertEquals(new BigDecimal("8500.00"), ah1.get().getBasePremium());
        assertEquals(12, ah1.get().getWaitingPeriodMonths());

        // Verify PLAN-A-H2 (Health 10L, Insurer 1, 14000 premium)
        Optional<Plan> ah2 = planRepository.findByCode("PLAN-A-H2");
        assertTrue(ah2.isPresent());
        assertEquals(new BigDecimal("14000.00"), ah2.get().getBasePremium());
        assertEquals(24, ah2.get().getWaitingPeriodMonths());

        // Verify PLAN-A-H3 (Unconfigured / No UW rules for TC-UW-03)
        Optional<Plan> ah3 = planRepository.findByCode("PLAN-A-H3");
        assertTrue(ah3.isPresent());
        assertNull(ah3.get().getUnderwritingRulesJson());

        // Verify PLAN-B-M1 (Insurer 2)
        Optional<Plan> bm1 = planRepository.findByCode("PLAN-B-M1");
        assertTrue(bm1.isPresent());
        assertEquals(2L, bm1.get().getInsurerId());

        // Verify PLAN-B-H1 (Insurer 2, 9200 premium)
        Optional<Plan> bh1 = planRepository.findByCode("PLAN-B-H1");
        assertTrue(bh1.isPresent());
        assertEquals(2L, bh1.get().getInsurerId());
        assertEquals(new BigDecimal("9200.00"), bh1.get().getBasePremium());
    }

    @Test
    void seederIsIdempotent() {
        seeder.run();
        assertEquals(7, planRepository.count());

        // Run second time; count must not double
        seeder.run();
        assertEquals(7, planRepository.count());
    }
}
