package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the canonical STP Section 6 plans, premium rules, and underwriting rules
 * into the database for development and automated testing environments.
 */
@Component
@Profile({"dev", "test"})
public class PlanDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanDataSeeder.class);
    private final PlanRepository planRepository;

    public PlanDataSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() > 0) {
            log.info("PlanDataSeeder: Plans table already contains data, skipping seed.");
            return;
        }

        log.info("PlanDataSeeder: Seeding STP Section 6 plans and rating rules...");

        // PLAN-A-M1 (Insurer A, 4-wheeler)
        Plan planAM1 = new Plan(
                "PLAN-A-M1",
                1L,
                "MOTOR_FOUR_WHEELER",
                "SafeShield Comprehensive Car",
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"ageLoadingMinYears\":6,\"ageLoadingMaxYears\":10,\"ageLoadingPct\":0.05,\"addOns\":{\"ZERO_DEP\":1080.00},\"depreciationPct\":0.10}",
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15,\"rejectIfActivePolicy\":true}",
                "[\"ZERO_DEP\",\"ROADSIDE_ASSISTANCE\"]",
                true
        );

        // PLAN-A-M2 (Insurer A, 2-wheeler)
        Plan planAM2 = new Plan(
                "PLAN-A-M2",
                1L,
                "MOTOR_TWO_WHEELER",
                "SafeShield Two-Wheeler Protect",
                new BigDecimal("100000.00"),
                new BigDecimal("500.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"ageLoadingMinYears\":6,\"ageLoadingMaxYears\":10,\"ageLoadingPct\":0.05,\"addOns\":{\"ZERO_DEP\":500.00},\"depreciationPct\":0.10}",
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15,\"rejectIfActivePolicy\":true}",
                "[\"ZERO_DEP\"]",
                true
        );

        // PLAN-A-H1 (Insurer A, health, premium 8,500)
        Plan planAH1 = new Plan(
                "PLAN-A-H1",
                1L,
                "HEALTH",
                "SafeShield Health Essential 5L",
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                12,
                new BigDecimal("8500.00"),
                "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00},{\"minAge\":36,\"maxAge\":45,\"base\":4500.00},{\"minAge\":46,\"maxAge\":59,\"base\":7000.00},{\"minAge\":60,\"maxAge\":120,\"base\":10000.00}],\"sumInsuredMultipliers\":{\"500000\":1.0,\"1000000\":1.6},\"preExistingMultiplier\":1.2}",
                "{\"maxAge\":70,\"referralAge\":60,\"preExistingReferral\":true}",
                "[\"CRITICAL_ILLNESS\"]",
                true
        );

        // PLAN-A-H2 (Insurer A, health, premium 14,000)
        Plan planAH2 = new Plan(
                "PLAN-A-H2",
                1L,
                "HEALTH",
                "SafeShield Health Advantage 10L",
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO,
                24,
                new BigDecimal("14000.00"),
                "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00},{\"minAge\":36,\"maxAge\":45,\"base\":4500.00},{\"minAge\":46,\"maxAge\":59,\"base\":7000.00},{\"minAge\":60,\"maxAge\":120,\"base\":10000.00}],\"sumInsuredMultipliers\":{\"500000\":1.0,\"1000000\":1.6},\"preExistingMultiplier\":1.2}",
                "{\"maxAge\":70,\"referralAge\":60,\"preExistingReferral\":true}",
                "[\"CRITICAL_ILLNESS\",\"MATERNITY_COVER\"]",
                true
        );

        // PLAN-A-H3 (Insurer A, health, no underwriting rules)
        Plan planAH3 = new Plan(
                "PLAN-A-H3",
                1L,
                "HEALTH",
                "SafeShield Health Unconfigured (No UW Rules)",
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                12,
                new BigDecimal("7500.00"),
                "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00}]}",
                null, // Explicitly null underwriting rules for TC-UW-03 testing
                "[]",
                true
        );

        // PLAN-B-M1 (Insurer B, 4-wheeler)
        Plan planBM1 = new Plan(
                "PLAN-B-M1",
                2L,
                "MOTOR_FOUR_WHEELER",
                "Bharat Care Car Supreme",
                new BigDecimal("600000.00"),
                new BigDecimal("2500.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"addOns\":{\"ZERO_DEP\":1200.00}}",
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15}",
                "[\"ZERO_DEP\"]",
                true
        );

        // PLAN-B-H1 (Insurer B, health, premium 9,200)
        Plan planBH1 = new Plan(
                "PLAN-B-H1",
                2L,
                "HEALTH",
                "Bharat Care Family Health",
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                12,
                new BigDecimal("9200.00"),
                "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3200.00}]}",
                "{\"maxAge\":70,\"referralAge\":60}",
                "[]",
                true
        );

        planRepository.saveAll(List.of(planAM1, planAM2, planAH1, planAH2, planAH3, planBM1, planBH1));
        log.info("PlanDataSeeder: Successfully seeded 7 STP Section 6 plans.");
    }
}
