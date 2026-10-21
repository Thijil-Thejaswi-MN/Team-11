package com.team11.insurance.plans.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload for POST /api/v1/plans creation (Contract 02 §4.3).
 */
public class PlanCreateResponse {

    @JsonProperty("planId")
    private Long planId;

    public PlanCreateResponse() {
    }

    public PlanCreateResponse(Long planId) {
        this.planId = planId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }
}
