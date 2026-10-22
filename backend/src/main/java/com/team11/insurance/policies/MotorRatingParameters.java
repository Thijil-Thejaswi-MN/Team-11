package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** Motor rating rule R-M of a plan (STP section 6), as plain numbers. Built by MotorRatingParametersParser. */
public record MotorRatingParameters(
        BigDecimal ownDamageBasePct,   // 0.02 = 2% of IDV
        int ageLoadingMinYears,        // loading applies from this vehicle age ...
        int ageLoadingMaxYears,        // ... to this age, both inclusive
        BigDecimal ageLoadingPct,      // 0.05 = 5% of the base own-damage premium
        Map<String, BigDecimal> addOnPrices,   // flat price per add-on, e.g. ZERO_DEP = 1080
        BigDecimal depreciationPct) {  // 0.10 = renewal IDV is previous IDV x 0.90

    public MotorRatingParameters {
        Objects.requireNonNull(ownDamageBasePct, "ownDamageBasePct");
        Objects.requireNonNull(ageLoadingPct, "ageLoadingPct");
        Objects.requireNonNull(depreciationPct, "depreciationPct");
        addOnPrices = addOnPrices == null ? Map.of() : Map.copyOf(addOnPrices);
    }
}
