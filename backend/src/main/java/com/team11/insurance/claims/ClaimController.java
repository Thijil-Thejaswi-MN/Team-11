package com.team11.insurance.claims;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.common.exception.AccessDeniedException;

/**
 * REST API Controller for Claims Core Workflow (Component C7 - SAD §4.1.3, §4.3.5, P4 Contract Section 5).
 * Base path context is /api/v1 (configured in application.yml).
 */
@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final IClaimService claimService;

    public ClaimController(IClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * Customer files a new claim against an active policy within 30 days of incident.
     */
    @PostMapping
    public ResponseEntity<ClaimResponse> fileClaim(
            @RequestBody ClaimSubmissionRequest request,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        ClaimResponse response = claimService.file(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves claim details and status history.
     * Customers may access their own claims; Claims Officers may access claims for their own insurer.
     * Fails closed when CurrentUser is missing.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClaimView> getClaim(
            @PathVariable("id") Long id,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        ClaimView view = claimService.get(id, currentUser);
        return ResponseEntity.ok(view);
    }

    /**
     * Lists claims belonging to a specific policy.
     */
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<ClaimView>> listByPolicy(
            @PathVariable("policyId") Long policyId,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        List<ClaimView> views = claimService.list(policyId, currentUser);
        return ResponseEntity.ok(views);
    }

    /**
     * Lists claims for insurer staff scoped to insurerId with optional status filter.
     */
    @GetMapping
    public ResponseEntity<List<ClaimView>> listForInsurer(
            @RequestParam(name = "insurerId", required = false) Long insurerId,
            @RequestParam(name = "status", required = false) ClaimStatus status,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        Long targetInsurerId = insurerId != null ? insurerId : currentUser.getInsurerId();
        List<ClaimView> views = claimService.listForInsurer(targetInsurerId, status, currentUser);
        return ResponseEntity.ok(views);
    }

    /**
     * Claims officer document verification.
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ClaimResponse> verifyDocuments(
            @PathVariable("id") Long id,
            @RequestBody ClaimVerificationRequest request,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        ClaimResponse response = claimService.verify(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Claims officer assessment decision (Approve or Reject).
     */
    @PostMapping("/{id}/decision")
    public ResponseEntity<ClaimResponse> recordDecision(
            @PathVariable("id") Long id,
            @RequestBody ClaimDecisionRequest request,
            CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        ClaimResponse response = claimService.decide(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
