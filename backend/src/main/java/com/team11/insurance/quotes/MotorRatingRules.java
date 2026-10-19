package com.team11.insurance.quotes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parsed configuration for motor rating rules (R-M), deserialized from plan premiumRulesJson.
 * Governs own-damage base percentage, age loading boundaries and percentage, and add-on pricing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MotorRatingRules {

    @JsonProperty("type")
    private String type = "R-M";

    @JsonProperty("ownDamageBasePct")
    private BigDecimal ownDamageBasePct = new BigDecimal("0.02");

    @JsonProperty("ageLoadingMinYears")
    private int ageLoadingMinYears = 6;

    @JsonProperty("ageLoadingMaxYears")
    private int ageLoadingMaxYears = 10;

    @JsonProperty("ageLoadingPct")
    private BigDecimal ageLoadingPct = new BigDecimal("0.05");

    @JsonProperty("addOns")
    private Map<String, BigDecimal> addOns = Collections.emptyMap();

    @JsonProperty("depreciationPct")
    private BigDecimal depreciationPct = new BigDecimal("0.10");

    public MotorRatingRules() {
    }

    public MotorRatingRules(BigDecimal ownDamageBasePct, int ageLoadingMinYears,
                            int ageLoadingMaxYears, BigDecimal ageLoadingPct,
                            Map<String, BigDecimal> addOns) {
        this.ownDamageBasePct = ownDamageBasePct != null ? ownDamageBasePct : new BigDecimal("0.02");
        this.ageLoadingMinYears = ageLoadingMinYears;
        this.ageLoadingMaxYears = ageLoadingMaxYears;
        this.ageLoadingPct = ageLoadingPct != null ? ageLoadingPct : new BigDecimal("0.05");
        this.addOns = addOns != null ? addOns : Collections.emptyMap();
    }

    /**
     * Calculates base premium from IDV: base = IDV * ownDamageBasePct.
     */
    public BigDecimal calculateBasePremium(BigDecimal idv) {
        if (idv == null || idv.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return idv.multiply(ownDamageBasePct).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates age loading: for vehicle ages from minYears through maxYears inclusive,
     * ageLoading = basePremium * ageLoadingPct; for all other ages, ageLoading = 0.
     */
    public BigDecimal calculateAgeLoading(BigDecimal basePremium, int vehicleAge) {
        if (basePremium == null || vehicleAge < ageLoadingMinYears || vehicleAge > ageLoadingMaxYears) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return basePremium.multiply(ageLoadingPct).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retrieves the configured flat fee for an add-on, or BigDecimal.ZERO if absent.
     */
    public BigDecimal getAddOnPrice(String addOnCode) {
        if (addOnCode == null || addOns == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal price = addOns.get(addOnCode.trim().toUpperCase());
        return price != null ? price.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean supportsAddOn(String addOnCode) {
        return addOnCode != null && addOns != null && addOns.containsKey(addOnCode.trim().toUpperCase());
    }

    public String getType() {
        return type;
    }

    public BigDecimal getOwnDamageBasePct() {
        return ownDamageBasePct;
    }

    public int getAgeLoadingMinYears() {
        return ageLoadingMinYears;
    }

    public int getAgeLoadingMaxYears() {
        return ageLoadingMaxYears;
    }

    public BigDecimal getAgeLoadingPct() {
        return ageLoadingPct;
    }

    public Map<String, BigDecimal> getAddOns() {
        return addOns;
    }

    public BigDecimal getDepreciationPct() {
        return depreciationPct;
    }
}
