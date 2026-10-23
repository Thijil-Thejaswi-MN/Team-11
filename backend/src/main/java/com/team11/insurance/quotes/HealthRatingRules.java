package com.team11.insurance.quotes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team11.insurance.common.exception.ValidationException;

/**
 * Parsed configuration for health rating rules (R-H), deserialized from plan premiumRulesJson.
 * Governs age-band base premiums, sum insured multipliers, and pre-existing disease loading (TC-Quote-02).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthRatingRules {

    @JsonProperty("type")
    private String type = "R-H";

    @JsonProperty("ageBands")
    private List<AgeBand> ageBands = defaultAgeBands();

    @JsonProperty("sumInsuredMultipliers")
    private Map<String, BigDecimal> sumInsuredMultipliers = defaultSumInsuredMultipliers();

    @JsonProperty("preExistingMultiplier")
    private BigDecimal preExistingMultiplier = new BigDecimal("1.2");

    public HealthRatingRules() {
    }

    public HealthRatingRules(List<AgeBand> ageBands, Map<String, BigDecimal> sumInsuredMultipliers, BigDecimal preExistingMultiplier) {
        this.ageBands = ageBands != null ? ageBands : defaultAgeBands();
        this.sumInsuredMultipliers = sumInsuredMultipliers != null ? sumInsuredMultipliers : defaultSumInsuredMultipliers();
        this.preExistingMultiplier = preExistingMultiplier != null ? preExistingMultiplier : new BigDecimal("1.2");
    }

    private static List<AgeBand> defaultAgeBands() {
        List<AgeBand> bands = new ArrayList<>();
        bands.add(new AgeBand(18, 35, new BigDecimal("3000.00")));
        bands.add(new AgeBand(36, 45, new BigDecimal("4500.00")));
        bands.add(new AgeBand(46, 59, new BigDecimal("7000.00")));
        bands.add(new AgeBand(60, 120, new BigDecimal("10000.00")));
        return bands;
    }

    private static Map<String, BigDecimal> defaultSumInsuredMultipliers() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("500000", new BigDecimal("1.0"));
        map.put("1000000", new BigDecimal("1.6"));
        return map;
    }

    /**
     * Looks up the base premium for an individual member based on age.
     */
    public BigDecimal calculateMemberBase(int age) {
        if (ageBands != null) {
            for (AgeBand band : ageBands) {
                if (age >= band.getMinAge() && age <= band.getMaxAge()) {
                    return band.getBase().setScale(2, RoundingMode.HALF_UP);
                }
            }
        }
        throw new ValidationException("No rating rule age band found for member age " + age + ".");
    }

    /**
     * Finds the multiplier for a given sum insured.
     * Throws ValidationException if the sum insured is not offered.
     */
    public BigDecimal getSumInsuredMultiplier(BigDecimal sumInsured) {
        if (sumInsured == null || sumInsured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Field 'sumInsured' must be greater than zero.");
        }
        if (sumInsuredMultipliers != null) {
            for (Map.Entry<String, BigDecimal> entry : sumInsuredMultipliers.entrySet()) {
                try {
                    BigDecimal configured = new BigDecimal(entry.getKey());
                    if (configured.compareTo(sumInsured) == 0) {
                        return entry.getValue();
                    }
                } catch (NumberFormatException ignored) {
                    if (entry.getKey().equalsIgnoreCase(sumInsured.toPlainString())) {
                        return entry.getValue();
                    }
                }
            }
        }
        throw new ValidationException("Sum insured " + sumInsured.toPlainString() + " is not offered by this plan.");
    }

    public BigDecimal getPreExistingMultiplier() {
        return preExistingMultiplier != null ? preExistingMultiplier : new BigDecimal("1.2");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AgeBand> getAgeBands() {
        return ageBands;
    }

    public void setAgeBands(List<AgeBand> ageBands) {
        this.ageBands = ageBands;
    }

    public Map<String, BigDecimal> getSumInsuredMultipliers() {
        return sumInsuredMultipliers;
    }

    public void setSumInsuredMultipliers(Map<String, BigDecimal> sumInsuredMultipliers) {
        this.sumInsuredMultipliers = sumInsuredMultipliers;
    }

    public void setPreExistingMultiplier(BigDecimal preExistingMultiplier) {
        this.preExistingMultiplier = preExistingMultiplier;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AgeBand {

        @JsonProperty("minAge")
        private int minAge;

        @JsonProperty("maxAge")
        private int maxAge;

        @JsonProperty("base")
        private BigDecimal base;

        public AgeBand() {
        }

        public AgeBand(int minAge, int maxAge, BigDecimal base) {
            this.minAge = minAge;
            this.maxAge = maxAge;
            this.base = base;
        }

        public int getMinAge() {
            return minAge;
        }

        public void setMinAge(int minAge) {
            this.minAge = minAge;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }

        public BigDecimal getBase() {
            return base;
        }

        public void setBase(BigDecimal base) {
            this.base = base;
        }
    }
}
