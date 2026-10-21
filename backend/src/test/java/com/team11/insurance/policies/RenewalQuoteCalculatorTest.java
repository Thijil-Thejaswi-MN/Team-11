package com.team11.insurance.policies;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.StateConflictException;

/** Fixture R-M for PLAN-A-M1 (STP section 6). TC-Ren-02 and TC-Ren-03. */
class RenewalQuoteCalculatorTest {

    private static final MotorRatingParameters PLAN_A_M1 = new MotorRatingParameters(
            new BigDecimal("0.02"), 6, 10, new BigDecimal("0.05"),
            Map.of("ZERO_DEP", new BigDecimal("1080.00")), new BigDecimal("0.10"));

    private static final BigDecimal PREVIOUS_IDV = new BigDecimal("400000");
    private static final LocalDate EXPIRY = LocalDate.of(2027, 11, 10);
    private static final LocalDate TODAY = EXPIRY.minusDays(20);          // clock year 2027, 20 days before expiry

    private static void assertRupees(long expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                "expected Rs " + expected + " but was " + actual);
    }

    private static MotorRenewalQuote quote(int regYear, List<String> addOns, boolean noClaim, LocalDate today) {
        return RenewalQuoteCalculator.quote(PLAN_A_M1, PREVIOUS_IDV, regYear, addOns, noClaim, EXPIRY, today);
    }

    @Test
    void noClaimRenewalGets10PercentDiscount_premium7884() {
        MotorRenewalQuote q = quote(2019, List.of("ZERO_DEP"), true, TODAY);
        assertEquals(8, q.vehicleAge());
        assertRupees(360000, q.newIdv());                  // 400,000 x 0.90
        assertRupees(7200, q.baseOwnDamage());             // 2% of 360,000
        assertRupees(360, q.ageLoading());                 // 5% of base, age 8 is within 6..10
        assertRupees(7560, q.ownDamageBeforeDiscount());
        assertRupees(756, q.noClaimDiscount());            // 10% of own-damage
        assertRupees(1080, q.addOnsTotal());               // ZERO_DEP flat
        assertRupees(7884, q.premium());
    }

    @Test
    void paidClaimInLastTermGetsNoDiscount_premium8640() {
        MotorRenewalQuote q = quote(2019, List.of("ZERO_DEP"), false, TODAY);
        assertRupees(0, q.noClaimDiscount());
        assertRupees(8640, q.premium());
    }

    @Test
    void ageLoadingAppliesFromAge6To10InclusiveOnly() {
        assertRupees(0, quote(2022, List.of(), true, TODAY).ageLoading());    // age 5
        assertRupees(360, quote(2021, List.of(), true, TODAY).ageLoading());  // age 6
        assertRupees(360, quote(2017, List.of(), true, TODAY).ageLoading());  // age 10
        assertRupees(0, quote(2016, List.of(), true, TODAY).ageLoading());    // age 11
    }

    @Test
    void youngVehicleWithoutAddOns() {
        // age 3: no loading; own-damage 7,200; discount 720; no add-ons -> 6,480
        assertRupees(6480, quote(2024, List.of(), true, TODAY).premium());
    }

    @Test
    void breakdownAddsUpToThePremium() {
        MotorRenewalQuote q = quote(2019, List.of("ZERO_DEP"), true, TODAY);
        assertEquals(0, q.premium().compareTo(
                q.ownDamageBeforeDiscount().subtract(q.noClaimDiscount()).add(q.addOnsTotal())));
    }

    @Test
    void renewableUntilIs15DaysAfterExpiry() {
        assertEquals(EXPIRY.plusDays(15), quote(2019, List.of(), true, TODAY).renewableUntil());
    }

    @Test
    void windowOpens30DaysBeforeAndClosesAfter15DaysGrace() {
        assertConflict(EXPIRY.minusDays(31));                                       // too early
        assertDoesNotThrow(() -> quote(2019, List.of(), true, EXPIRY.minusDays(30)));
        assertDoesNotThrow(() -> quote(2019, List.of(), true, EXPIRY.plusDays(15))); // grace period
        assertConflict(EXPIRY.plusDays(16));                                        // must buy a new policy
    }

    private static void assertConflict(LocalDate today) {
        StateConflictException ex = assertThrows(StateConflictException.class,
                () -> quote(2019, List.of(), true, today));
        assertEquals(ErrorCode.STATE_409, ex.getErrorCode());
    }

    @Test
    void unknownAddOnAndBadInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> quote(2019, List.of("NOT_OFFERED"), true, TODAY));
        assertThrows(IllegalArgumentException.class, () -> quote(2030, List.of(), true, TODAY));  // future regYear
        assertThrows(IllegalArgumentException.class, () -> RenewalQuoteCalculator.quote(
                PLAN_A_M1, BigDecimal.ZERO, 2019, List.of(), true, EXPIRY, TODAY));
        assertThrows(IllegalArgumentException.class, () -> RenewalQuoteCalculator.quote(
                PLAN_A_M1, new BigDecimal("-100"), 2019, List.of(), true, EXPIRY, TODAY));
        assertThrows(IllegalArgumentException.class, () -> RenewalQuoteCalculator.quote(
                PLAN_A_M1, null, 2019, List.of(), true, EXPIRY, TODAY));
    }

    @Test
    void nullAddOnPricesInMotorRatingParametersDefaultsToEmptyMap() {
        MotorRatingParameters params = new MotorRatingParameters(
                new BigDecimal("0.02"), 6, 10, new BigDecimal("0.05"),
                null, new BigDecimal("0.10"));
        assertNotNull(params.addOnPrices());
        assertTrue(params.addOnPrices().isEmpty());

        MotorRenewalQuote q = RenewalQuoteCalculator.quote(params, PREVIOUS_IDV, 2019, List.of(), true, EXPIRY, TODAY);
        assertRupees(0, q.addOnsTotal());
    }

    @Test
    void emptyAddOnPricesMapWorksAsExpected() {
        MotorRatingParameters params = new MotorRatingParameters(
                new BigDecimal("0.02"), 6, 10, new BigDecimal("0.05"),
                Map.of(), new BigDecimal("0.10"));
        assertTrue(params.addOnPrices().isEmpty());

        MotorRenewalQuote q = RenewalQuoteCalculator.quote(params, PREVIOUS_IDV, 2019, List.of(), true, EXPIRY, TODAY);
        assertRupees(0, q.addOnsTotal());
    }

    @Test
    void nullAddOnsCollectionInQuoteDefaultsToZero() {
        MotorRenewalQuote q = RenewalQuoteCalculator.quote(PLAN_A_M1, PREVIOUS_IDV, 2019, null, true, EXPIRY, TODAY);
        assertRupees(0, q.addOnsTotal());
    }

    @Test
    void nullDatePreconditionsInRequireWithinWindowAndQuote() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> RenewalQuoteCalculator.requireWithinWindow(null, TODAY));
        assertEquals("expiryDate and today must not be null", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> RenewalQuoteCalculator.requireWithinWindow(EXPIRY, null));
        assertEquals("expiryDate and today must not be null", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> RenewalQuoteCalculator.quote(PLAN_A_M1, PREVIOUS_IDV, 2019, List.of(), true, null, TODAY));
        assertEquals("expiryDate and today must not be null", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class,
                () -> RenewalQuoteCalculator.quote(PLAN_A_M1, PREVIOUS_IDV, 2019, List.of(), true, EXPIRY, null));
        assertEquals("expiryDate and today must not be null", ex4.getMessage());
    }

    @Test
    void nullRulesPreconditionInQuote() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RenewalQuoteCalculator.quote(null, PREVIOUS_IDV, 2019, List.of(), true, EXPIRY, TODAY));
        assertEquals("rules must not be null", ex.getMessage());
    }
}
