package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.team11.insurance.common.exception.StateConflictException;

/**
 * Motor renewal quote (INS-F-019; SRS 4.6.1; SAD 4.1.6 steps 1, 3, 4, 5). Pure functions.
 *
 *  1. Window: from 30 days before expiry to 15 days after (inclusive), otherwise 409 STATE-409.
 *  2. New IDV = previous IDV x (1 - depreciationPct), i.e. x 0.90 for the seeded plans.
 *  3. Vehicle age = year of "today" - registration year (STP section 6 uses the clock year).
 *  4. Base own-damage = new IDV x ownDamageBasePct.
 *  5. Age loading = base x ageLoadingPct when the age is within the min..max range (inclusive).
 *  6. Own-damage = base + loading. A 10% no-claim discount applies to the own-damage premium only
 *     when there was no claim in the previous term.
 *  7. Premium = own-damage - discount + add-ons (flat prices from the rule).
 *
 * D-09.5 rule (docs/decisions.md D-09.5 and docs/contracts/p3-contract.md §3.4):
 * noClaimInPreviousTerm is true unless the policy had at least one claim with status SETTLED
 * (IClaimService.hasPaidClaim(policyId)). Claims in SUBMITTED, UNDER_REVIEW, APPROVED, or
 * REJECTED do NOT count as paid and do NOT forfeit the 10% No-Claim Discount. Also note that
 * all rupee steps use Rupees.ROUNDING (RoundingMode.DOWN to whole rupees per SRS §4.6.2,
 * SAD §4.1.6, and p3-contract.md §2).
 *
 * Every money step is rounded with Rupees.round, so the breakdown always adds up to the premium.
 * The spec does not state a rounding rule for premiums; this mirrors the refund rule (down), and the
 * motor quote in INS-F-005 must round the same way.
 */
public final class RenewalQuoteCalculator {

    public static final int WINDOW_DAYS_BEFORE_EXPIRY = 30;
    public static final int GRACE_DAYS_AFTER_EXPIRY = 15;
    public static final BigDecimal NO_CLAIM_DISCOUNT_PCT = new BigDecimal("0.10");

    private RenewalQuoteCalculator() { }

    /** @throws StateConflictException (STATE-409) when today is outside the renewal window */
    public static void requireWithinWindow(LocalDate expiryDate, LocalDate today) {
        if (expiryDate == null || today == null) {
            throw new IllegalArgumentException("expiryDate and today must not be null");
        }
        LocalDate opens = expiryDate.minusDays(WINDOW_DAYS_BEFORE_EXPIRY);
        LocalDate closes = expiryDate.plusDays(GRACE_DAYS_AFTER_EXPIRY);
        if (today.isBefore(opens) || today.isAfter(closes)) {
            throw new StateConflictException("Policy cannot be renewed on " + today
                    + ": the renewal window is " + opens + " to " + closes);
        }
    }

    public static MotorRenewalQuote quote(MotorRatingParameters rules, BigDecimal previousIdv,
                                          int registrationYear, Collection<String> addOns,
                                          boolean noClaimInPreviousTerm, LocalDate expiryDate,
                                          LocalDate today) {
        requireWithinWindow(expiryDate, today);

        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        if (previousIdv == null || previousIdv.signum() <= 0) {
            throw new IllegalArgumentException("previousIdv must be positive");
        }
        int vehicleAge = today.getYear() - registrationYear;
        if (vehicleAge < 0) {
            throw new IllegalArgumentException("registration year " + registrationYear + " is in the future");
        }

        BigDecimal newIdv = Rupees.round(previousIdv.multiply(BigDecimal.ONE.subtract(rules.depreciationPct())));
        BigDecimal base = Rupees.round(newIdv.multiply(rules.ownDamageBasePct()));
        boolean loadingApplies = vehicleAge >= rules.ageLoadingMinYears() && vehicleAge <= rules.ageLoadingMaxYears();
        BigDecimal loading = loadingApplies ? Rupees.round(base.multiply(rules.ageLoadingPct())) : BigDecimal.ZERO;
        BigDecimal ownDamage = base.add(loading);
        BigDecimal discount = noClaimInPreviousTerm
                ? Rupees.round(ownDamage.multiply(NO_CLAIM_DISCOUNT_PCT)) : BigDecimal.ZERO;

        BigDecimal addOnsTotal = BigDecimal.ZERO;
        Set<String> distinctAddOns = new LinkedHashSet<>(addOns == null ? Set.of() : addOns);
        for (String addOn : distinctAddOns) {
            BigDecimal price = rules.addOnPrices().get(addOn);
            if (price == null) {
                throw new IllegalArgumentException("add-on " + addOn + " is not offered by this plan");
            }
            addOnsTotal = addOnsTotal.add(price);
        }
        addOnsTotal = Rupees.round(addOnsTotal);

        BigDecimal premium = ownDamage.subtract(discount).add(addOnsTotal);
        return new MotorRenewalQuote(newIdv, vehicleAge, base, loading, ownDamage, discount, addOnsTotal,
                premium, expiryDate.plusDays(GRACE_DAYS_AFTER_EXPIRY));
    }
}
