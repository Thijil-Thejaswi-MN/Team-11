package com.team11.insurance.applications.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

/** Body of POST /applications (contract docs/contracts/p3-contract.md section 3.1). */
public record SubmitApplicationRequest(
        @NotNull Long quoteId,
        @NotNull Map<String, Object> details,   // product form data, for example { "vehicleRegNo": "KA22AB1234" }
        List<Long> documentIds) { }             // checked in the service so KYC (403) is decided first
