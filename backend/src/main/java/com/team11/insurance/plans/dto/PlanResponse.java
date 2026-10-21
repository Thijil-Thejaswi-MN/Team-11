package com.team11.insurance.plans.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing an insurance plan response (SAD §4.3.2, INS-F-003, INS-F-004).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanResponse {

    @JsonProperty("planId")
    private Long planId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("insurerId")
    private Long insurerId;

    @JsonProperty("insurer")
    private String insurer;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;

    @JsonProperty("coverage")
    private BigDecimal coverage;

    @JsonProperty("deductible")
    private BigDecimal deductible;

    @JsonProperty("waitingPeriod")
    private Integer waitingPeriod;

    @JsonProperty("waitingPeriodMonths")
    private Integer waitingPeriodMonths;

    @JsonProperty("basePremium")
    private BigDecimal basePremium;

    @JsonProperty("premiumRulesJson")
    private String premiumRulesJson;

    @JsonProperty("underwritingRulesJson")
    private String underwritingRulesJson;

    @JsonProperty("addOns")
    private List<String> addOns;

    @JsonProperty("active")
    private boolean active;

    public PlanResponse() {
    }

    public PlanResponse(Long planId, String code, Long insurerId, String insurer, String type,
                        String name, BigDecimal coverage, BigDecimal deductible,
                        Integer waitingPeriodMonths, BigDecimal basePremium,
                        String premiumRulesJson, String underwritingRulesJson,
                        List<String> addOns, boolean active) {
        this.planId = planId;
        this.code = code;
        this.insurerId = insurerId;
        this.insurer = insurer;
        this.type = type;
        this.name = name;
        this.coverage = coverage;
        this.deductible = deductible;
        this.waitingPeriod = waitingPeriodMonths;
        this.waitingPeriodMonths = waitingPeriodMonths;
        this.basePremium = basePremium;
        this.premiumRulesJson = premiumRulesJson;
        this.underwritingRulesJson = underwritingRulesJson;
        this.addOns = addOns != null ? addOns : Collections.emptyList();
        this.active = active;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }

    public String getInsurer() {
        return insurer;
    }

    public void setInsurer(String insurer) {
        this.insurer = insurer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getWaitingPeriod() {
        return waitingPeriod;
    }

    public void setWaitingPeriod(Integer waitingPeriod) {
        this.waitingPeriod = waitingPeriod;
        this.waitingPeriodMonths = waitingPeriod;
    }

    public Integer getWaitingPeriodMonths() {
        return waitingPeriodMonths;
    }

    public void setWaitingPeriodMonths(Integer waitingPeriodMonths) {
        this.waitingPeriodMonths = waitingPeriodMonths;
        this.waitingPeriod = waitingPeriodMonths;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
