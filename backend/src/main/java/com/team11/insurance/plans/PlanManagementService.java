package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.dto.CreatePlanRequest;
import com.team11.insurance.plans.dto.PatchPlanRequest;
import com.team11.insurance.plans.dto.PlanCompareResponse;
import com.team11.insurance.plans.dto.PlanCreateResponse;
import com.team11.insurance.plans.dto.PlanListResponse;
import com.team11.insurance.plans.dto.PlanResponse;
import com.team11.insurance.plans.dto.UpdatePlanRequest;

/**
 * Service managing plan lifecycle operations, public plan browsing, and comparisons
 * (INS-F-003, INS-F-004, INS-F-022, Contract 02 §4).
 */
@Service
@Transactional
public class PlanManagementService {

    private static final Logger log = LoggerFactory.getLogger(PlanManagementService.class);

    private final PlanRepository planRepository;
    private final InsurerRepository insurerRepository;
    private final ObjectMapper objectMapper;
    private final com.team11.insurance.admin.IAuditLog auditLog;

    @org.springframework.beans.factory.annotation.Autowired
    public PlanManagementService(PlanRepository planRepository,
                                 InsurerRepository insurerRepository,
                                 ObjectMapper objectMapper,
                                 com.team11.insurance.admin.IAuditLog auditLog) {
        this.planRepository = planRepository;
        this.insurerRepository = insurerRepository;
        this.objectMapper = objectMapper;
        this.auditLog = auditLog;
    }

    public PlanManagementService(PlanRepository planRepository,
                                 InsurerRepository insurerRepository,
                                 ObjectMapper objectMapper) {
        this(planRepository, insurerRepository, objectMapper, null);
    }

    /**
     * Creates an insurance plan (INS-F-003, Contract 02 §4.3).
     * Restricted to INSURER_ADMIN and SYSTEM_ADMIN.
     */
    public PlanCreateResponse createPlan(CreatePlanRequest request, CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.INSURER_ADMIN, UserRole.SYSTEM_ADMIN);

        validateCreateRequest(request);

        Long insurerId;
        if (currentUser.isSystemAdmin()) {
            insurerId = request.getInsurerId() != null ? request.getInsurerId() : 1L;
        } else {
            insurerId = currentUser.getInsurerId();
            if (insurerId == null) {
                throw new AccessDeniedException("Insurer Admin is not linked to any insurer.");
            }
        }

        if (planRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new StateConflictException("Plan with code '" + request.getCode().trim() + "' already exists.");
        }

        String normalizedType = normalizePlanType(request.getType());
        String addOnsJson = serializeAddOns(request.getAddOns());

        Plan plan = new Plan(
                request.getCode().trim().toUpperCase(),
                insurerId,
                normalizedType,
                request.getName().trim(),
                request.getCoverage(),
                request.getDeductible(),
                request.getWaitingPeriodMonths() != null ? request.getWaitingPeriodMonths() : 0,
                request.getBasePremium(),
                request.getPremiumRulesJson(),
                request.getUnderwritingRulesJson(),
                addOnsJson,
                request.getActive() != null ? request.getActive() : true
        );

        Plan savedPlan = planRepository.save(plan);
        log.info("Plan created: id={}, code={}, insurerId={}, type={}",
                savedPlan.getId(), savedPlan.getCode(), savedPlan.getInsurerId(), savedPlan.getType());

        if (auditLog != null && currentUser != null) {
            auditLog.record(currentUser.getUserId(), "PLAN_CREATED", "Plan", savedPlan.getId(),
                    "Plan created: " + savedPlan.getCode() + ", name: " + savedPlan.getName());
        }

        return new PlanCreateResponse(savedPlan.getId());
    }

    /**
     * Updates an existing insurance plan (full PUT update).
     */
    public PlanResponse updatePlan(Long planId, UpdatePlanRequest request, CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.INSURER_ADMIN, UserRole.SYSTEM_ADMIN);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));

        RoleSecurity.assertInsurerAccess(currentUser, plan.getInsurerId());
        validateUpdateRequest(request);

        plan.setName(request.getName().trim());
        plan.setType(normalizePlanType(request.getType()));
        plan.setCoverage(request.getCoverage());
        plan.setDeductible(request.getDeductible());
        plan.setWaitingPeriodMonths(request.getWaitingPeriodMonths() != null ? request.getWaitingPeriodMonths() : 0);
        plan.setBasePremium(request.getBasePremium());
        if (request.getPremiumRulesJson() != null) {
            plan.setPremiumRulesJson(request.getPremiumRulesJson());
        }
        if (request.getUnderwritingRulesJson() != null) {
            plan.setUnderwritingRulesJson(request.getUnderwritingRulesJson());
        }
        if (request.getAddOns() != null) {
            plan.setAddOnsJson(serializeAddOns(request.getAddOns()));
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        Plan savedPlan = planRepository.save(plan);
        log.info("Plan updated: id={}, code={}", savedPlan.getId(), savedPlan.getCode());

        if (auditLog != null && currentUser != null) {
            auditLog.record(currentUser.getUserId(), "PLAN_EDITED", "Plan", savedPlan.getId(),
                    "Plan updated: " + savedPlan.getCode() + ", name: " + savedPlan.getName());
        }

        return toPlanResponse(savedPlan);
    }

    /**
     * Partially updates an existing plan or deactivates it (PATCH /plans/{id}).
     */
    public PlanResponse patchPlan(Long planId, PatchPlanRequest request, CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.INSURER_ADMIN, UserRole.SYSTEM_ADMIN);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));

        RoleSecurity.assertInsurerAccess(currentUser, plan.getInsurerId());

        if (request.getName() != null && !request.getName().isBlank()) {
            plan.setName(request.getName().trim());
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            plan.setType(normalizePlanType(request.getType()));
        }
        if (request.getCoverage() != null) {
            if (request.getCoverage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Coverage amount must be greater than zero.");
            }
            plan.setCoverage(request.getCoverage());
        }
        if (request.getDeductible() != null) {
            if (request.getDeductible().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Deductible cannot be negative.");
            }
            plan.setDeductible(request.getDeductible());
        }
        if (request.getWaitingPeriodMonths() != null) {
            if (request.getWaitingPeriodMonths() < 0) {
                throw new ValidationException("Waiting period cannot be negative.");
            }
            plan.setWaitingPeriodMonths(request.getWaitingPeriodMonths());
        }
        if (request.getBasePremium() != null) {
            if (request.getBasePremium().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Base premium cannot be negative.");
            }
            plan.setBasePremium(request.getBasePremium());
        }
        if (request.getPremiumRulesJson() != null) {
            plan.setPremiumRulesJson(request.getPremiumRulesJson());
        }
        if (request.getUnderwritingRulesJson() != null) {
            plan.setUnderwritingRulesJson(request.getUnderwritingRulesJson());
        }
        if (request.getAddOns() != null) {
            plan.setAddOnsJson(serializeAddOns(request.getAddOns()));
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        Plan savedPlan = planRepository.save(plan);
        log.info("Plan patched: id={}, code={}, active={}", savedPlan.getId(), savedPlan.getCode(), savedPlan.isActive());

        if (auditLog != null && currentUser != null) {
            if (request.getActive() != null && !request.getActive()) {
                auditLog.record(currentUser.getUserId(), "PLAN_DEACTIVATED", "Plan", savedPlan.getId(),
                        "Plan deactivated: " + savedPlan.getCode());
            } else if (request.getActive() != null && request.getActive()) {
                auditLog.record(currentUser.getUserId(), "PLAN_ACTIVATED", "Plan", savedPlan.getId(),
                        "Plan activated: " + savedPlan.getCode());
            } else {
                auditLog.record(currentUser.getUserId(), "PLAN_EDITED", "Plan", savedPlan.getId(),
                        "Plan partially updated: " + savedPlan.getCode());
            }
        }

        return toPlanResponse(savedPlan);
    }

    /**
     * Public plan browsing with filtering and pagination (INS-F-004, INS-F-022, Contract 02 §4.1).
     * Only returns active plans.
     */
    @Transactional(readOnly = true)
    public PlanListResponse getPublicPlans(String type, Long insurerId, BigDecimal premiumMax,
                                          BigDecimal coverageMin, Integer page, Integer size) {
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 20;

        List<Plan> activePlans = planRepository.findByActiveTrue();

        // Apply filters in-memory
        List<Plan> filtered = activePlans.stream()
                .filter(p -> {
                    if (type != null && !type.isBlank()) {
                        String filterType = normalizePlanType(type);
                        if (!p.getType().equalsIgnoreCase(filterType)) {
                            return false;
                        }
                    }
                    if (insurerId != null) {
                        if (!p.getInsurerId().equals(insurerId)) {
                            return false;
                        }
                    }
                    if (premiumMax != null) {
                        if (p.getBasePremium().compareTo(premiumMax) > 0) {
                            return false;
                        }
                    }
                    if (coverageMin != null) {
                        if (p.getCoverage().compareTo(coverageMin) < 0) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<PlanResponse> pageItems = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());

        return new PlanListResponse(pageItems, total, pageNum, pageSize);
    }

    /**
     * Public plan comparison for up to 3 plans (INS-F-004, Contract 02 §4.2, TC-Plan-02).
     */
    @Transactional(readOnly = true)
    public PlanCompareResponse comparePlans(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            throw new IllegalArgumentException("At least one plan ID is required for comparison.");
        }
        if (planIds.size() > 3) {
            throw new IllegalArgumentException("Cannot compare more than 3 plans. Requested: " + planIds.size());
        }

        List<PlanResponse> plans = new ArrayList<>();
        for (Long id : planIds) {
            Plan plan = planRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
            plans.add(toPlanResponse(plan));
        }

        return new PlanCompareResponse(plans);
    }

    /**
     * Retrieves a single plan by ID.
     */
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
        return toPlanResponse(plan);
    }

    public PlanResponse toPlanResponse(Plan plan) {
        String insurerName = null;
        if (plan.getInsurerId() != null) {
            Optional<Insurer> insurerOpt = insurerRepository.findById(plan.getInsurerId());
            insurerName = insurerOpt.map(Insurer::getName).orElse("Insurer " + plan.getInsurerId());
        }

        List<String> addOns = parseAddOns(plan.getAddOnsJson());

        return new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getInsurerId(),
                insurerName,
                plan.getType(),
                plan.getName(),
                plan.getCoverage(),
                plan.getDeductible(),
                plan.getWaitingPeriodMonths(),
                plan.getBasePremium(),
                plan.getPremiumRulesJson(),
                plan.getUnderwritingRulesJson(),
                addOns,
                plan.isActive()
        );
    }

    private void validateCreateRequest(CreatePlanRequest request) {
        if (request == null) {
            throw new ValidationException("Plan creation request body is missing.");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new ValidationException("Field 'code' is required.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Field 'name' is required.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ValidationException("Field 'type' is required.");
        }
        if (request.getCoverage() == null || request.getCoverage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Field 'coverage' must be greater than zero.");
        }
        if (request.getDeductible() != null && request.getDeductible().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Field 'deductible' cannot be negative.");
        }
        if (request.getBasePremium() != null && request.getBasePremium().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Field 'basePremium' cannot be negative.");
        }
        if (request.getWaitingPeriodMonths() != null && request.getWaitingPeriodMonths() < 0) {
            throw new ValidationException("Field 'waitingPeriodMonths' cannot be negative.");
        }
    }

    private void validateUpdateRequest(UpdatePlanRequest request) {
        if (request == null) {
            throw new ValidationException("Plan update request body is missing.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Field 'name' is required.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ValidationException("Field 'type' is required.");
        }
        if (request.getCoverage() == null || request.getCoverage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Field 'coverage' must be greater than zero.");
        }
        if (request.getDeductible() != null && request.getDeductible().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Field 'deductible' cannot be negative.");
        }
        if (request.getBasePremium() != null && request.getBasePremium().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Field 'basePremium' cannot be negative.");
        }
        if (request.getWaitingPeriodMonths() != null && request.getWaitingPeriodMonths() < 0) {
            throw new ValidationException("Field 'waitingPeriodMonths' cannot be negative.");
        }
    }

    private String normalizePlanType(String type) {
        if (type == null || type.isBlank()) {
            return "HEALTH";
        }
        String t = type.trim().toUpperCase();
        if ("4W".equals(t) || "MOTOR_FOUR_WHEELER".equals(t)) {
            return "MOTOR_FOUR_WHEELER";
        }
        if ("2W".equals(t) || "MOTOR_TWO_WHEELER".equals(t)) {
            return "MOTOR_TWO_WHEELER";
        }
        if ("HEALTH".equals(t)) {
            return "HEALTH";
        }
        return t;
    }

    private String serializeAddOns(List<String> addOns) {
        if (addOns == null || addOns.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(addOns);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize add-ons list: {}", addOns, e);
            return "[]";
        }
    }

    private List<String> parseAddOns(String addOnsJson) {
        if (addOnsJson == null || addOnsJson.isBlank() || "[]".equals(addOnsJson.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(addOnsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse addOnsJson '{}': {}", addOnsJson, e.getMessage());
            return Collections.emptyList();
        }
    }
}
