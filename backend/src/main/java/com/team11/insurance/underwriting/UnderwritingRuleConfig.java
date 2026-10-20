package com.team11.insurance.underwriting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parsed configuration models for motor and health plan underwriting rules (SAD §4.3.7, SRS §4.4.1).
 */
public final class UnderwritingRuleConfig {

    private UnderwritingRuleConfig() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MotorRules {
        @JsonProperty("maxVehicleAgeYears")
        private Integer maxVehicleAgeYears;

        @JsonProperty("referralVehicleAgeYears")
        private Integer referralVehicleAgeYears;

        @JsonProperty("rejectIfActivePolicy")
        private Boolean rejectIfActivePolicy = true;

        public MotorRules() {}

        public MotorRules(Integer maxVehicleAgeYears, Integer referralVehicleAgeYears, Boolean rejectIfActivePolicy) {
            this.maxVehicleAgeYears = maxVehicleAgeYears;
            this.referralVehicleAgeYears = referralVehicleAgeYears;
            this.rejectIfActivePolicy = rejectIfActivePolicy != null ? rejectIfActivePolicy : true;
        }

        public Integer getMaxVehicleAgeYears() {
            return maxVehicleAgeYears;
        }

        public void setMaxVehicleAgeYears(Integer maxVehicleAgeYears) {
            this.maxVehicleAgeYears = maxVehicleAgeYears;
        }

        public Integer getReferralVehicleAgeYears() {
            return referralVehicleAgeYears;
        }

        public void setReferralVehicleAgeYears(Integer referralVehicleAgeYears) {
            this.referralVehicleAgeYears = referralVehicleAgeYears;
        }

        public Boolean getRejectIfActivePolicy() {
            return rejectIfActivePolicy != null ? rejectIfActivePolicy : true;
        }

        public void setRejectIfActivePolicy(Boolean rejectIfActivePolicy) {
            this.rejectIfActivePolicy = rejectIfActivePolicy;
        }

        public boolean isValid() {
            return maxVehicleAgeYears != null && referralVehicleAgeYears != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HealthRules {
        @JsonProperty("maxAge")
        private Integer maxAge;

        @JsonProperty("referralAge")
        private Integer referralAge;

        @JsonProperty("preExistingReferral")
        private Boolean preExistingReferral = true;

        public HealthRules() {}

        public HealthRules(Integer maxAge, Integer referralAge, Boolean preExistingReferral) {
            this.maxAge = maxAge;
            this.referralAge = referralAge;
            this.preExistingReferral = preExistingReferral != null ? preExistingReferral : true;
        }

        public Integer getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Integer maxAge) {
            this.maxAge = maxAge;
        }

        public Integer getReferralAge() {
            return referralAge;
        }

        public void setReferralAge(Integer referralAge) {
            this.referralAge = referralAge;
        }

        public Boolean getPreExistingReferral() {
            return preExistingReferral != null ? preExistingReferral : true;
        }

        public void setPreExistingReferral(Boolean preExistingReferral) {
            this.preExistingReferral = preExistingReferral;
        }

        public boolean isValid() {
            return maxAge != null && referralAge != null;
        }
    }
}
