package com.team11.insurance.underwriting.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationStatus;

/**
 * Response DTO for referred applications awaiting underwriter review (INS-F-011, SAD §4.3.7).
 * Encapsulates the application fields required for underwriting risk assessment while avoiding
 * direct exposure of internal JPA entity state (e.g. database timestamps, Hibernate proxies).
 */
public record ReferredApplicationResponse(
        Long id,
        Long customerId,
        Long insurerId,
        Long planId,
        Long quoteId,
        String productType,
        String vehicleRegNo,
        BigDecimal premium,
        String detailsJson,
        ApplicationStatus status,
        String reason,
        Instant createdAt
) {
    public static ReferredApplicationResponse from(Application app) {
        if (app == null) {
            return null;
        }
        return new ReferredApplicationResponse(
                app.getId(),
                app.getCustomerId(),
                app.getInsurerId(),
                app.getPlanId(),
                app.getQuoteId(),
                app.getProductType(),
                app.getVehicleRegNo(),
                app.getPremium(),
                app.getDetailsJson(),
                app.getStatus(),
                app.getRejectionReason(),
                app.getCreatedAt()
        );
    }
}
