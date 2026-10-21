package com.team11.insurance.quotes.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for health insurance quote calculation (API Contract §2.2, SAD §4.3.3, TC-Quote-02).
 */
public class HealthQuoteRequest {

    @JsonProperty("planId")
    @NotNull(message = "Field 'planId' is required.")
    private Long planId;

    @JsonProperty("members")
    @NotNull(message = "Field 'members' is required.")
    @NotEmpty(message = "Field 'members' must contain at least one member.")
    @Valid
    private List<HealthMemberRequest> members;

    @JsonProperty("sumInsured")
    @NotNull(message = "Field 'sumInsured' is required.")
    private BigDecimal sumInsured;

    @JsonProperty("preExisting")
    private Boolean preExisting;

    public HealthQuoteRequest() {
    }

    public HealthQuoteRequest(Long planId, List<HealthMemberRequest> members, BigDecimal sumInsured, Boolean preExisting) {
        this.planId = planId;
        this.members = members;
        this.sumInsured = sumInsured;
        this.preExisting = preExisting;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public List<HealthMemberRequest> getMembers() {
        return members;
    }

    public void setMembers(List<HealthMemberRequest> members) {
        this.members = members;
    }

    public BigDecimal getSumInsured() {
        return sumInsured;
    }

    public void setSumInsured(BigDecimal sumInsured) {
        this.sumInsured = sumInsured;
    }

    public Boolean getPreExisting() {
        return preExisting;
    }

    public boolean isPreExisting() {
        return Boolean.TRUE.equals(preExisting);
    }

    public void setPreExisting(Boolean preExisting) {
        this.preExisting = preExisting;
    }
}
