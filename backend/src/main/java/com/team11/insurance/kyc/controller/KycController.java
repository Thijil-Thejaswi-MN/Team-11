package com.team11.insurance.kyc.controller;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.dto.KycStatusResponse;
import com.team11.insurance.kyc.entity.KycRecord;
import com.team11.insurance.kyc.service.KycService;
import com.team11.insurance.common.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for KYC verification endpoints (INS-F-002, SAD §4.3.1, API Contract §3).
 * <p>
 * Base path: {@code /kyc} (serviced under servlet context-path {@code /api/v1}).
 */
@RestController
@RequestMapping("/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    /**
     * Retrieves the authenticated customer's KYC verification status (API Contract §3.2).
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser,
                UserRole.CUSTOMER, UserRole.INSURER_ADMIN, UserRole.UNDERWRITER, UserRole.CLAIMS_OFFICER, UserRole.SYSTEM_ADMIN);

        KycStatus status = kycService.getStatus(currentUser.getUserId());
        return ResponseEntity.ok(new KycStatusResponse(status.getDisplayValue()));
    }

    /**
     * Submits KYC verification details for the authenticated customer (API Contract §3.1).
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submitKyc(@RequestBody(required = false) Map<String, String> payload,
                                                        CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        String panNumber = payload != null ? payload.get("panNumber") : null;
        String aadhaarNumber = payload != null ? payload.get("aadhaarNumber") : null;

        if (panNumber == null || panNumber.trim().isEmpty()) {
            throw new ValidationException("panNumber", "panNumber is required");
        }
        if (aadhaarNumber == null || aadhaarNumber.trim().isEmpty()) {
            throw new ValidationException("aadhaarNumber", "aadhaarNumber is required");
        }

        KycRecord record = kycService.submitKyc(currentUser.getUserId(), panNumber.trim(), aadhaarNumber.trim(), null);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("kycStatus", record.getStatus().getDisplayValue()));
    }
}
