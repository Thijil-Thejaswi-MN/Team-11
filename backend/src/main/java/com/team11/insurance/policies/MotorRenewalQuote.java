package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Result of a motor renewal quote, with the breakdown the API returns (contract p3-contract.md 3.4). */
public record MotorRenewalQuote(
        BigDecimal newIdv,
        int vehicleAge,
        BigDecimal baseOwnDamage,
        BigDecimal ageLoading,
        BigDecimal ownDamageBeforeDiscount,
        BigDecimal noClaimDiscount,
        BigDecimal addOnsTotal,
        BigDecimal premium,
        LocalDate renewableUntil) { }
