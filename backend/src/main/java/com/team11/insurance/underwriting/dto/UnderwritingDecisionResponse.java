package com.team11.insurance.underwriting.dto;

import com.team11.insurance.applications.ApplicationStatus;

/**
 * Response payload confirming recorded manual underwriting decision (INS-F-011).
 */
public record UnderwritingDecisionResponse(
        Long applicationId,
        ApplicationStatus status,
        String reason
) {}
