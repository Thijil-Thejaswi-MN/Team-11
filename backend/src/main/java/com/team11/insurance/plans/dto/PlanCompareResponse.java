package com.team11.insurance.plans.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for GET /plans/compare responses (Contract 02 §4.2, TC-Plan-02).
 */
public class PlanCompareResponse {

    @JsonProperty("plans")
    private List<PlanResponse> plans = new ArrayList<>();

    public PlanCompareResponse() {
    }

    public PlanCompareResponse(List<PlanResponse> plans) {
        this.plans = plans != null ? plans : new ArrayList<>();
    }

    public List<PlanResponse> getPlans() {
        return plans;
    }

    public void setPlans(List<PlanResponse> plans) {
        this.plans = plans;
    }
}
