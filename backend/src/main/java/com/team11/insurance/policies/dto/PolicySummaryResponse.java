package com.team11.insurance.policies.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyStatus;

/**
 * DTO representing an issued policy summary response (P3-05 / INS-F-024).
 */
public record PolicySummaryResponse(
        Long id,
        String policyNo,
        Long applicationId,
        Long previousPolicyId,
        Long customerId,
        Long insurerId,
        Long planId,
        String vehicleRegNo,
        BigDecimal premiumPaid,
        LocalDate startDate,
        LocalDate endDate,
        PolicyStatus status,
        Long documentFileId,
        Instant createdAt
) {
    public static PolicySummaryResponse from(Policy policy) {
        return new PolicySummaryResponse(
                policy.getId(),
                policy.getPolicyNo(),
                policy.getApplicationId(),
                policy.getPreviousPolicyId(),
                policy.getCustomerId(),
                policy.getInsurerId(),
                policy.getPlanId(),
                policy.getVehicleRegNo(),
                policy.getPremiumPaid(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getStatus(),
                policy.getDocumentFileId(),
                policy.getCreatedAt()
        );
    }
}
