package com.team11.insurance.plans.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload for creating an insurance plan via POST /api/v1/plans (Contract 02 §4.3, INS-F-003).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePlanRequest {

    @NotBlank(message = "Plan code is required")
    @JsonProperty("code")
    private String code;

    @NotBlank(message = "Plan type is required")
    @JsonProperty("type")
    private String type;

    @NotBlank(message = "Plan name is required")
    @JsonProperty("name")
    private String name;

    @NotNull(message = "Coverage amount is required")
    @Positive(message = "Coverage amount must be greater than zero")
    @JsonProperty("coverage")
    private BigDecimal coverage;

    @NotNull(message = "Deductible is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Deductible must be non-negative")
    @JsonProperty("deductible")
    private BigDecimal deductible = BigDecimal.ZERO;

    @DecimalMin(value = "0", inclusive = true, message = "Waiting period must be non-negative")
    @JsonProperty("waitingPeriodMonths")
    private Integer waitingPeriodMonths = 0;

    @NotNull(message = "Base premium is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Base premium must be non-negative")
    @JsonProperty("basePremium")
    private BigDecimal basePremium = BigDecimal.ZERO;

    @JsonProperty("premiumRulesJson")
    private String premiumRulesJson;

    @JsonProperty("underwritingRulesJson")
    private String underwritingRulesJson;

    @JsonProperty("addOns")
    private List<String> addOns = new ArrayList<>();

    @JsonProperty("active")
    private Boolean active = true;

    /**
     * Optional insurerId override for SYSTEM_ADMIN; INSURER_ADMIN derives insurerId from session.
     */
    @JsonProperty("insurerId")
    private Long insurerId;

    public CreatePlanRequest() {
    }

    public CreatePlanRequest(String code, String type, String name, BigDecimal coverage,
                             BigDecimal deductible, Integer waitingPeriodMonths,
                             BigDecimal basePremium, String premiumRulesJson,
                             String underwritingRulesJson, List<String> addOns, Boolean active) {
        this.code = code;
        this.type = type;
        this.name = name;
        this.coverage = coverage;
        this.deductible = deductible != null ? deductible : BigDecimal.ZERO;
        this.waitingPeriodMonths = waitingPeriodMonths != null ? waitingPeriodMonths : 0;
        this.basePremium = basePremium != null ? basePremium : BigDecimal.ZERO;
        this.premiumRulesJson = premiumRulesJson;
        this.underwritingRulesJson = underwritingRulesJson;
        this.addOns = addOns != null ? addOns : new ArrayList<>();
        this.active = active != null ? active : true;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
        this.addOns = addOns != null ? addOns : new ArrayList<>();
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }
}
