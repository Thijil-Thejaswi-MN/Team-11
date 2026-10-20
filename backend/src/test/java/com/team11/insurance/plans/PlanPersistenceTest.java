package com.team11.insurance.plans;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Persistence tests for {@link Plan} entity and {@link PlanRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanPersistenceTest {

    @Autowired
    private PlanRepository planRepository;

    @Test
    void canPersistAndQueryPlanWithRules() {
        Plan plan = new Plan(
                "TEST-PLAN-01",
                99L,
                "TEST_TYPE_01",
                "Test Car Plan",
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02}",
                "{\"maxVehicleAgeYears\":20}",
                "[\"ZERO_DEP\"]",
                true
        );

        Plan saved = planRepository.save(plan);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<Plan> retrieved = planRepository.findByCode("TEST-PLAN-01");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Car Plan", retrieved.get().getName());
        assertEquals(new BigDecimal("500000.00"), retrieved.get().getCoverage());
        assertEquals(99L, retrieved.get().getInsurerId());
        assertTrue(retrieved.get().isActive());
    }

    @Test
    void queriesByInsurerAndType() {
        Plan p1 = new Plan("TEST-M1", 99L, "TEST_FILTER_4W", "Motor 1",
                new BigDecimal("400000.00"), new BigDecimal("1000.00"), 0, BigDecimal.ZERO, null, null, null, true);
        Plan p2 = new Plan("TEST-H1", 99L, "TEST_FILTER_HEALTH", "Health 1",
                new BigDecimal("500000.00"), BigDecimal.ZERO, 12, new BigDecimal("8000.00"), null, null, null, true);
        Plan p3 = new Plan("TEST-M2", 98L, "TEST_FILTER_4W", "Motor 2",
                new BigDecimal("450000.00"), new BigDecimal("1500.00"), 0, BigDecimal.ZERO, null, null, null, false);

        planRepository.saveAll(List.of(p1, p2, p3));

        List<Plan> insurer1Plans = planRepository.findByInsurerId(99L);
        assertEquals(2, insurer1Plans.size());

        List<Plan> activeMotor = planRepository.findByTypeAndActiveTrue("TEST_FILTER_4W");
        assertEquals(1, activeMotor.size());
        assertEquals("TEST-M1", activeMotor.get(0).getCode());
    }
}
