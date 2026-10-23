package com.team11.insurance.plans.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/**
 * Payload for partial update or deactivation of a plan via PATCH /api/v1/plans/{id} (Contract 02 §4.4, TC-Plan-01).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatchPlanRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @Positive(message = "Coverage amount must be greater than zero")
    @JsonProperty("coverage")
    private BigDecimal coverage;

    @DecimalMin(value = "0.0", inclusive = true, message = "Deductible must be non-negative")
    @JsonProperty("deductible")
    private BigDecimal deductible;

    @DecimalMin(value = "0", inclusive = true, message = "Waiting period must be non-negative")
    @JsonProperty("waitingPeriodMonths")
    private Integer waitingPeriodMonths;

    @DecimalMin(value = "0.0", inclusive = true, message = "Base premium must be non-negative")
    @JsonProperty("basePremium")
    private BigDecimal basePremium;

    @JsonProperty("premiumRulesJson")
    private String premiumRulesJson;

    @JsonProperty("underwritingRulesJson")
    private String underwritingRulesJson;

    @JsonProperty("addOns")
    private List<String> addOns;

    @JsonProperty("active")
    private Boolean active;

    public PatchPlanRequest() {
    }

    public PatchPlanRequest(Boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getCoverage() {
        return coverage;
    }

    public void setCoverage(BigDecimal coverage) {
        this.coverage = coverage;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public Integer getWaitingPeriodMonths() {
        return waitingPeriodMonths;
    }

    public void setWaitingPeriodMonths(Integer waitingPeriodMonths) {
        this.waitingPeriodMonths = waitingPeriodMonths;
    }

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    public String getPremiumRulesJson() {
        return premiumRulesJson;
    }

    public void setPremiumRulesJson(String premiumRulesJson) {
        this.premiumRulesJson = premiumRulesJson;
    }

    public String getUnderwritingRulesJson() {
        return underwritingRulesJson;
    }

    public void setUnderwritingRulesJson(String underwritingRulesJson) {
        this.underwritingRulesJson = underwritingRulesJson;
    }

    public List<String> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<String> addOns) {
        this.addOns = addOns;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
