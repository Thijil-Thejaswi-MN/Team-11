package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Refund on cancellation (INS-F-020; SRS 4.6.2; SAD 4.1.6, which is the test oracle). Pure functions: no
 * Spring, no database, no clock, so they are easy to unit-test.
 *
 * Fee = 5% of the premium paid for the current term.
 *   - a claim has been paid                  -> 0
 *   - day of term <= 15 (free-look), no claim -> premiumPaid - fee
 *   - later, no claim                         -> max(0, premiumPaid x unusedDays / totalDays - fee)
 * The result is rounded with Rupees.ROUNDING (down). The fee itself is not rounded; only the final refund is.
 *
 * Day counting (matches the STP examples): the start date is day 1, so "cancelled on day 100" of a
 * 365-day term has unusedDays = 365 - 100 = 265.
 */
public final class RefundCalculator {

    public static final BigDecimal FEE_RATE = new BigDecimal("0.05");
    public static final int FREE_LOOK_DAYS = 15;

    private RefundCalculator() { }

    /** Date-based entry point; both term dates are inclusive. */
    public static BigDecimal calculateRefund(BigDecimal premiumPaid, LocalDate termStart, LocalDate termEnd,
                                             LocalDate cancellationDate, boolean claimPaid) {
        int totalDays = (int) ChronoUnit.DAYS.between(termStart, termEnd) + 1;
        int dayOfTerm = (int) ChronoUnit.DAYS.between(termStart, cancellationDate) + 1;
        return calculateRefund(premiumPaid, dayOfTerm, totalDays, claimPaid);
    }

    /** @throws IllegalArgumentException if the inputs are impossible (the caller maps this to an error) */
    public static BigDecimal calculateRefund(BigDecimal premiumPaid, int dayOfTerm, int totalDays,
                                             boolean claimPaid) {
        if (premiumPaid == null || premiumPaid.signum() < 0) {
            throw new IllegalArgumentException("premiumPaid must be zero or more");
        }
        if (totalDays < 1) {
            throw new IllegalArgumentException("totalDays must be at least 1");
        }
        if (dayOfTerm < 1 || dayOfTerm > totalDays) {
            throw new IllegalArgumentException(
                    "cancellation day " + dayOfTerm + " is outside the term (1 to " + totalDays + ")");
        }
        if (claimPaid) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = premiumPaid.multiply(FEE_RATE);
        if (dayOfTerm <= FREE_LOOK_DAYS) {
            return Rupees.round(premiumPaid.subtract(fee));
        }
        int unusedDays = totalDays - dayOfTerm;
        // exact arithmetic: (premium x unused - fee x total) / total, rounded once at the end
        BigDecimal numerator = premiumPaid.multiply(BigDecimal.valueOf(unusedDays))
                .subtract(fee.multiply(BigDecimal.valueOf(totalDays)));
        if (numerator.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(BigDecimal.valueOf(totalDays), 0, Rupees.ROUNDING);
    }
}
