package com.team11.insurance.payments.dto;

import java.math.BigDecimal;

/**
 * Summary details of an application for customer payment review (P4-17).
 */
public record ApplicationPaymentSummary(
        Long applicationId,
        Long customerId,
        Long planId,
        String planName,
        String productType,
        String vehicleRegNo,
        BigDecimal premium,
        String status
) {}
