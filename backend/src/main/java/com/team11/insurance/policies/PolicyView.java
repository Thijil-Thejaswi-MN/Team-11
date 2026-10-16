package com.team11.insurance.policies;

import java.time.LocalDate;

/** Read-only view of a policy handed to other modules (Claims, Underwriting) through IPolicyQuery. */
public record PolicyView(
        long id,
        String policyNo,
        long customerId,
        long insurerId,
        long planId,
        String vehicleRegNo,   // null for health policies
        LocalDate startDate,
        LocalDate endDate,
        PolicyStatus status) { }
