package com.team11.insurance.underwriting;

import java.time.Clock;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStateMachine;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.HealthRules;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.MotorRules;

/**
 * Production implementation of {@link IUnderwriting} (SAD §4.3.7, SRS §4.4.1, INS-F-010, INS-F-011).
 * <p>
 * Applies data-driven underwriting rules for motor and health applications using configured plan rules.
 * Handles missing rule configurations safely by referring applications for manual underwriter assessment
 * rather than silently approving them.
 */
@Service
@Primary
public class UnderwritingEngine implements IUnderwriting {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingEngine.class);
    public static final String MISSING_CONFIG_REASON = "Missing rule configuration";

    private final IPlanQuery planQuery;
    private final MotorUnderwritingEvaluator motorEvaluator;
    private final HealthUnderwritingEvaluator healthEvaluator;
    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final ObjectProvider<IAuditLog> auditLogProvider;
    private final ObjectProvider<Clock> clockProvider;
    private final ObjectMapper objectMapper;

    public UnderwritingEngine(IPlanQuery planQuery,
                              MotorUnderwritingEvaluator motorEvaluator,
                              HealthUnderwritingEvaluator healthEvaluator,
                              ApplicationRepository applicationRepository,
                              ApplicationHistoryRepository historyRepository,
                              ObjectProvider<IAuditLog> auditLogProvider,
                              ObjectProvider<Clock> clockProvider,
                              ObjectMapper objectMapper) {
        this.planQuery = Objects.requireNonNull(planQuery, "planQuery must not be null");
        this.motorEvaluator = Objects.requireNonNull(motorEvaluator, "motorEvaluator must not be null");
        this.healthEvaluator = Objects.requireNonNull(healthEvaluator, "healthEvaluator must not be null");
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "applicationRepository must not be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository must not be null");
        this.auditLogProvider = auditLogProvider;
        this.clockProvider = clockProvider;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public UnderwritingResult evaluate(Application application) {
        if (application == null) {
            log.warn("UnderwritingEngine.evaluate called with null application");
            return UnderwritingResult.refer("Missing application");
        }

        if (application.getPlanId() == null) {
            log.warn("UnderwritingEngine: Application ID {} has no plan ID", application.getId());
            return UnderwritingResult.refer(MISSING_CONFIG_REASON);
        }

        // 1. Retrieve the associated plan and its underwriting rules
        Plan plan;
        try {
            plan = planQuery.getPlan(application.getPlanId());
        } catch (Exception e) {
            log.warn("UnderwritingEngine: Plan ID {} could not be found for application {}: {}",
                    application.getPlanId(), application.getId(), e.getMessage());
            return UnderwritingResult.refer(MISSING_CONFIG_REASON);
        }

        if (plan == null) {
            log.warn("UnderwritingEngine: Plan ID {} is null for application {}",
                    application.getPlanId(), application.getId());
            return UnderwritingResult.refer(MISSING_CONFIG_REASON);
        }

        String rulesJson = plan.getUnderwritingRulesJson();
        if (rulesJson == null || rulesJson.isBlank() || "null".equalsIgnoreCase(rulesJson.trim())) {
            log.info("UnderwritingEngine: Plan '{}' (ID {}) has no underwriting rules configured",
                    plan.getCode(), plan.getId());
            return UnderwritingResult.refer(MISSING_CONFIG_REASON);
        }

        // 2. Resolve product type
        String productType = resolveProductType(application, plan);
        Clock effectiveClock = clockProvider != null ? clockProvider.getIfAvailable(Clock::systemDefaultZone) : Clock.systemDefaultZone();

        // 3. Delegate to appropriate product rule evaluator
        if (isMotorProduct(productType)) {
            MotorRules motorRules = parseMotorRules(rulesJson);
            if (motorRules == null || !motorRules.isValid()) {
                log.warn("UnderwritingEngine: Invalid or missing motor rule configuration for plan {}", plan.getCode());
                return UnderwritingResult.refer(MISSING_CONFIG_REASON);
            }
            return motorEvaluator.evaluate(application, motorRules, effectiveClock);
        } else if (isHealthProduct(productType)) {
            HealthRules healthRules = parseHealthRules(rulesJson);
            if (healthRules == null || !healthRules.isValid()) {
                log.warn("UnderwritingEngine: Invalid or missing health rule configuration for plan {}", plan.getCode());
                return UnderwritingResult.refer(MISSING_CONFIG_REASON);
            }
            return healthEvaluator.evaluate(application, healthRules);
        } else {
            log.warn("UnderwritingEngine: Unrecognized product type '{}' for application {}",
                    productType, application.getId());
            return UnderwritingResult.refer(MISSING_CONFIG_REASON);
        }
    }

    @Override
    @Transactional
    public void recordManualDecision(long applicationId, Decision decision, String reason, long underwriterId) {
        if (decision == null) {
            throw new IllegalArgumentException("Decision must not be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason must not be blank for manual decision");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        ApplicationStatus targetStatus = decision == Decision.APPROVE
                ? ApplicationStatus.APPROVED
                : ApplicationStatus.REJECTED;

        // Enforce state transition (REFERRED -> APPROVED or REFERRED -> REJECTED)
        ApplicationStateMachine.requireTransition(application.getStatus(), targetStatus);

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(targetStatus);
        if (targetStatus == ApplicationStatus.REJECTED) {
            application.setRejectionReason(reason);
        }
        applicationRepository.save(application);

        // Record state transition in immutable application history
        historyRepository.save(new ApplicationHistory(
                applicationId,
                previousStatus,
                targetStatus,
                underwriterId,
                reason
        ));

        // Record audit trail event
        if (auditLogProvider != null) {
            IAuditLog auditLog = auditLogProvider.getIfAvailable();
            if (auditLog != null) {
                auditLog.record(underwriterId, "UNDERWRITING_MANUAL_" + decision, "Application", applicationId, reason);
            }
        }

        log.info("UnderwritingEngine: Recorded manual decision for application {}: {} by underwriter {}. Reason: {}",
                applicationId, decision, underwriterId, reason);
    }

    private String resolveProductType(Application application, Plan plan) {
        if (application.getProductType() != null && !application.getProductType().isBlank()) {
            return application.getProductType().toUpperCase();
        }
        if (plan.getType() != null && !plan.getType().isBlank()) {
            return plan.getType().toUpperCase();
        }
        return "";
    }

    private boolean isMotorProduct(String productType) {
        return productType.contains("MOTOR")
                || productType.contains("TWO_WHEELER")
                || productType.contains("FOUR_WHEELER")
                || productType.equals("2W")
                || productType.equals("4W");
    }

    private boolean isHealthProduct(String productType) {
        return productType.contains("HEALTH");
    }

    private MotorRules parseMotorRules(String rulesJson) {
        try {
            return objectMapper.readValue(rulesJson, MotorRules.class);
        } catch (Exception e) {
            log.warn("Failed to parse MotorRules JSON: {}", e.getMessage());
            return null;
        }
    }

    private HealthRules parseHealthRules(String rulesJson) {
        try {
            return objectMapper.readValue(rulesJson, HealthRules.class);
        } catch (Exception e) {
            log.warn("Failed to parse HealthRules JSON: {}", e.getMessage());
            return null;
        }
    }
}
