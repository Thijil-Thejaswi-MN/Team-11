package com.team11.insurance.kyc.controller;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.dto.KycStatusResponse;
import com.team11.insurance.kyc.service.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * Retrieves the authenticated customer's KYC verification status (API Contract 02 §3.2).
     * Strictly restricted to CUSTOMER role; staff and system admins are prohibited.
     * Derives identity strictly from trusted server-side authenticated CurrentUser context.
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);

        KycStatus status = kycService.getStatus(currentUser.getUserId());
        return ResponseEntity.ok(new KycStatusResponse(status.getDisplayValue()));
    }

    // NOTE: POST /api/v1/kyc (multipart document submission per API Contract 02 §3.1 and INS-SR-007)
    // is safely deferred pending merge and availability of the agreed IFileStore storage contract
    // (Component C12 / Person 4, PR #127 / issue #127). The prior incomplete stub accepting JSON
    // map and storing null documentPath was removed to prevent security vulnerabilities and contract violations.
}
