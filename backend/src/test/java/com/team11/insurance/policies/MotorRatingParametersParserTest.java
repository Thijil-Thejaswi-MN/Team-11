package com.team11.insurance.policies;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MotorRatingParametersParserTest {

    /** The exact premium-rule JSON that PlanDataSeeder stores for PLAN-A-M1. */
    private static final String PLAN_A_M1 =
            "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"ageLoadingMinYears\":6,\"ageLoadingMaxYears\":10,"
                    + "\"ageLoadingPct\":0.05,\"addOns\":{\"ZERO_DEP\":1080.00},\"depreciationPct\":0.10}";

    @Test
    void parsesTheSeededMotorRule() {
        MotorRatingParameters p = MotorRatingParametersParser.fromJson(PLAN_A_M1);
        assertEquals(0, new BigDecimal("0.02").compareTo(p.ownDamageBasePct()));
        assertEquals(6, p.ageLoadingMinYears());
        assertEquals(10, p.ageLoadingMaxYears());
        assertEquals(0, new BigDecimal("0.05").compareTo(p.ageLoadingPct()));
        assertEquals(0, new BigDecimal("1080").compareTo(p.addOnPrices().get("ZERO_DEP")));
        assertEquals(0, new BigDecimal("0.10").compareTo(p.depreciationPct()));
    }

    @Test
    void parsedRuleGivesTheTcRen02Premium() {
        MotorRatingParameters p = MotorRatingParametersParser.fromJson(PLAN_A_M1);
        java.time.LocalDate expiry = java.time.LocalDate.of(2027, 11, 10);
        MotorRenewalQuote q = RenewalQuoteCalculator.quote(p, new BigDecimal("400000"), 2019,
                java.util.List.of("ZERO_DEP"), true, expiry, expiry.minusDays(20));
        assertEquals(0, new BigDecimal("7884").compareTo(q.premium()));
    }

    @Test
    void rejectsNonMotorOrBrokenRules() {
        assertThrows(IllegalArgumentException.class, () -> MotorRatingParametersParser.fromJson("{\"type\":\"R-H\"}"));
        assertThrows(IllegalArgumentException.class, () -> MotorRatingParametersParser.fromJson("not json"));
        assertThrows(IllegalArgumentException.class, () -> MotorRatingParametersParser.fromJson(
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02}"));       // missing fields
    }
}
