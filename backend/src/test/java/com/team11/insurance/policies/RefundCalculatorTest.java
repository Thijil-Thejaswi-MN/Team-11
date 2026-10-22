package com.team11.insurance.policies;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Test oracle: SRS 4.6.2 and SAD 4.1.6 (premium Rs 12,000, 365-day term, fee 5% = Rs 600). TC-Can-01, TC-Can-02. */
class RefundCalculatorTest {

    private static final BigDecimal PREMIUM = new BigDecimal("12000.00");

    private static void assertRupees(long expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                "expected Rs " + expected + " but was " + actual);
    }

    @Test
    void freeLookDay10GivesPremiumMinusFee() {
        assertRupees(11400, RefundCalculator.calculateRefund(PREMIUM, 10, 365, false));
    }

    @Test
    void lastFreeLookDay15StillGivesPremiumMinusFee() {
        assertRupees(11400, RefundCalculator.calculateRefund(PREMIUM, 15, 365, false));
    }

    @Test
    void day16IsProRataRoundedDown() {
        // 12,000 x 349 / 365 = 11,473.97; minus 600 = 10,873.97; rounded DOWN = 10,873.
        // NOTE: STP TC-Can-02 and the Day-4 plan say 10,874, and show 11,474.52 as the intermediate value,
        // which is an arithmetic error (the true value is 11,473.97). 10,874 is only reached by rounding to
        // the nearest rupee, which contradicts "all refund amounts are rounded down" (SRS 4.6.2).
        // If the team decides to round to nearest, change Rupees.ROUNDING and this expected value together.
        assertRupees(10873, RefundCalculator.calculateRefund(PREMIUM, 16, 365, false));
    }

    @Test
    void day100MatchesTheWorkedExample() {
        assertRupees(8112, RefundCalculator.calculateRefund(PREMIUM, 100, 365, false));
    }

    @Test
    void day360WouldBeNegativeSoRefundIsZero() {
        assertRupees(0, RefundCalculator.calculateRefund(PREMIUM, 360, 365, false));
    }

    @Test
    void paidClaimGivesZeroEvenInTheFreeLookPeriod() {
        assertRupees(0, RefundCalculator.calculateRefund(PREMIUM, 10, 365, true));
        assertRupees(0, RefundCalculator.calculateRefund(PREMIUM, 100, 365, true));
    }

    @Test
    void otherPremiumsAreRoundedDownToTheRupee() {
        // 10,000 x 265 / 365 = 7,260.27; minus fee 500 = 6,760.27; rounded down = 6,760
        assertRupees(6760, RefundCalculator.calculateRefund(new BigDecimal("10000"), 100, 365, false));
    }

    @Test
    void dateBasedOverloadCountsTheStartDateAsDay1() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2027, 10, 19);                       // 365 days, both dates inclusive
        assertRupees(11400, RefundCalculator.calculateRefund(PREMIUM, start, end, start.plusDays(9), false));   // day 10
        assertRupees(8112, RefundCalculator.calculateRefund(PREMIUM, start, end, start.plusDays(99), false));   // day 100
    }

    @Test
    void impossibleInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, 0, 365, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, 366, 365, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, 10, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> RefundCalculator.calculateRefund(new BigDecimal("-1"), 10, 365, false));
    }

    @Test
    void isPaidClaimStatusSettledOnly() {
        assertTrue(RefundCalculator.isPaidClaimStatus("SETTLED"));
        assertTrue(RefundCalculator.isPaidClaimStatus("settled"));
        assertTrue(RefundCalculator.isPaidClaimStatus(" SETTLED "));
        assertFalse(RefundCalculator.isPaidClaimStatus("APPROVED"));
        assertFalse(RefundCalculator.isPaidClaimStatus("SUBMITTED"));
        assertFalse(RefundCalculator.isPaidClaimStatus("UNDER_REVIEW"));
        assertFalse(RefundCalculator.isPaidClaimStatus("PAYMENT_PROCESSING"));
        assertFalse(RefundCalculator.isPaidClaimStatus("REJECTED"));
        assertFalse(RefundCalculator.isPaidClaimStatus(null));
        assertFalse(RefundCalculator.isPaidClaimStatus(""));
    }

    @Test
    void leapYearPolicyTerm366Days() {
        LocalDate start = LocalDate.of(2028, 1, 1);
        LocalDate end = LocalDate.of(2028, 12, 31); // 366 days in leap year
        // Day 15 (free look): 12000 - 600 = 11400
        assertRupees(11400, RefundCalculator.calculateRefund(PREMIUM, start, end, start.plusDays(14), false));
        // Day 100: unused = 266 days, total = 366 days.
        // (12000 * 266 - 600 * 366) / 366 = (3192000 - 219600) / 366 = 2972400 / 366 = 8121.31 -> 8121
        assertRupees(8121, RefundCalculator.calculateRefund(PREMIUM, start, end, start.plusDays(99), false));
    }

    @Test
    void nullInputsInDateBasedRefundThrow() {
        LocalDate start = LocalDate.of(2026, 10, 20);
        LocalDate end = LocalDate.of(2027, 10, 19);
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(null, start, end, start, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, null, end, start, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, start, null, start, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, start, end, null, false));
        assertThrows(IllegalArgumentException.class, () -> RefundCalculator.calculateRefund(PREMIUM, end, start, start, false)); // end before start
    }
}
