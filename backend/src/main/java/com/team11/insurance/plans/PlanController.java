package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.plans.dto.CreatePlanRequest;
import com.team11.insurance.plans.dto.PatchPlanRequest;
import com.team11.insurance.plans.dto.PlanCompareResponse;
import com.team11.insurance.plans.dto.PlanCreateResponse;
import com.team11.insurance.plans.dto.PlanListResponse;
import com.team11.insurance.plans.dto.PlanResponse;
import com.team11.insurance.plans.dto.UpdatePlanRequest;

import jakarta.validation.Valid;

/**
 * REST Controller for Insurance Plans management, public browsing, and comparison
 * (INS-F-003, INS-F-004, INS-F-022, Contract 02 §4).
 */
@RestController
@RequestMapping("/plans")
public class PlanController {

    private static final Logger log = LoggerFactory.getLogger(PlanController.class);

    private final PlanManagementService planManagementService;

    public PlanController(PlanManagementService planManagementService) {
        this.planManagementService = planManagementService;
    }

    /**
     * Public endpoint to browse active insurance plans (INS-F-004, INS-F-022, Contract 02 §4.1).
     */
    @GetMapping
    public ResponseEntity<PlanListResponse> getPlans(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "insurer", required = false) Long insurerId,
            @RequestParam(name = "premiumMax", required = false) BigDecimal premiumMax,
            @RequestParam(name = "coverageMin", required = false) BigDecimal coverageMin,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size) {

        log.debug("GET /plans query: type={}, insurer={}, premiumMax={}, coverageMin={}, page={}, size={}",
                type, insurerId, premiumMax, coverageMin, page, size);

        PlanListResponse response = planManagementService.getPublicPlans(type, insurerId, premiumMax, coverageMin, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Public endpoint to compare up to 3 plans side by side (INS-F-004, Contract 02 §4.2, TC-Plan-02).
     */
    @GetMapping("/compare")
    public ResponseEntity<PlanCompareResponse> comparePlans(
            @RequestParam(name = "ids") List<Long> ids) {

        log.debug("GET /plans/compare requested for plan IDs: {}", ids);

        PlanCompareResponse response = planManagementService.comparePlans(ids);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves details for a specific plan.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable("id") Long id) {
        PlanResponse response = planManagementService.getPlanById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Insurer Admin endpoint to create a new insurance plan (INS-F-003, Contract 02 §4.3, TC-Plan-01).
     */
    @PostMapping
    public ResponseEntity<PlanCreateResponse> createPlan(
            @Valid @RequestBody CreatePlanRequest request,
            CurrentUser currentUser) {

        log.info("POST /plans request by userId={}: code={}, type={}",
                currentUser != null ? currentUser.getUserId() : null, request.getCode(), request.getType());

        PlanCreateResponse response = planManagementService.createPlan(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Insurer Admin endpoint to update an existing plan (Contract 02 §4.4, TC-Plan-01).
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, PlanResponse>> updatePlan(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePlanRequest request,
            CurrentUser currentUser) {

        log.info("PUT /plans/{} request by userId={}", id, currentUser != null ? currentUser.getUserId() : null);

        PlanResponse updated = planManagementService.updatePlan(id, request, currentUser);
        return ResponseEntity.ok(Map.of("plan", updated));
    }

    /**
     * Insurer Admin endpoint to partially update or deactivate a plan (Contract 02 §4.4, TC-Plan-01).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, PlanResponse>> patchPlan(
            @PathVariable("id") Long id,
            @RequestBody PatchPlanRequest request,
            CurrentUser currentUser) {

        log.info("PATCH /plans/{} request by userId={}: active={}",
                id, currentUser != null ? currentUser.getUserId() : null, request.getActive());

        PlanResponse patched = planManagementService.patchPlan(id, request, currentUser);
        return ResponseEntity.ok(Map.of("plan", patched));
    }
}
