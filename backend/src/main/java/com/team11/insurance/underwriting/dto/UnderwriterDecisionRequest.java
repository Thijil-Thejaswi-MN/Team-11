package com.team11.insurance.underwriting.dto;

import com.team11.insurance.underwriting.Decision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for underwriter manual decision on a referred application (INS-F-011, SAD §4.3.7).
 */
public class UnderwriterDecisionRequest {

    @NotNull(message = "Decision (APPROVE or REJECT) is mandatory")
    private Decision decision;

    @NotBlank(message = "Reason is mandatory for manual underwriter decision")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public UnderwriterDecisionRequest() {}

    public UnderwriterDecisionRequest(Decision decision, String reason) {
        this.decision = decision;
        this.reason = reason;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
