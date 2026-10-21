package com.team11.insurance.quotes.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Breakdown of calculated premium components (SAD §4.3.3, §4.3.7, TC-Quote-01).
 */
public class PremiumBreakdown {

    @JsonProperty("basePremium")
    private BigDecimal basePremium;

    @JsonProperty("ageLoading")
    private BigDecimal ageLoading;

    @JsonProperty("addOns")
    private Map<String, BigDecimal> addOns;

    @JsonProperty("totalPremium")
    private BigDecimal totalPremium;

    public PremiumBreakdown() {
    }

    public PremiumBreakdown(BigDecimal basePremium, BigDecimal ageLoading,
                            Map<String, BigDecimal> addOns, BigDecimal totalPremium) {
        this.basePremium = basePremium;
        this.ageLoading = ageLoading;
        this.addOns = addOns != null ? addOns : Collections.emptyMap();
        this.totalPremium = totalPremium;
    }

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    public BigDecimal getAgeLoading() {
        return ageLoading;
    }

    public void setAgeLoading(BigDecimal ageLoading) {
        this.ageLoading = ageLoading;
    }

    public Map<String, BigDecimal> getAddOns() {
        return addOns;
    }

    public void setAddOns(Map<String, BigDecimal> addOns) {
        this.addOns = addOns;
    }

    public BigDecimal getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(BigDecimal totalPremium) {
        this.totalPremium = totalPremium;
    }
}
