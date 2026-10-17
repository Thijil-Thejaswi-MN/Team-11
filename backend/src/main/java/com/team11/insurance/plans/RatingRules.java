package com.team11.insurance.plans;

/**
 * Data structure holding premium rating rules and underwriting rules for a plan
 * (SAD 4.3.2, 4.3.7, and API contracts).
 */
public class RatingRules {

    private final Long planId;
    private final String planCode;
    private final String ruleType;
    private final String premiumRulesJson;
    private final String underwritingRulesJson;

    public RatingRules(Long planId, String planCode, String ruleType,
                       String premiumRulesJson, String underwritingRulesJson) {
        this.planId = planId;
        this.planCode = planCode;
        this.ruleType = ruleType;
        this.premiumRulesJson = premiumRulesJson;
        this.underwritingRulesJson = underwritingRulesJson;
    }

    public Long getPlanId() {
        return planId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getPremiumRulesJson() {
        return premiumRulesJson;
    }

    public String getUnderwritingRulesJson() {
        return underwritingRulesJson;
    }

    public boolean hasUnderwritingRules() {
        return underwritingRulesJson != null && !underwritingRulesJson.isBlank();
    }
}
