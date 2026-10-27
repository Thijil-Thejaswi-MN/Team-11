package com.team11.insurance.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.admin.AuditLog;
import com.team11.insurance.admin.AuditLogRepository;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.notifications.NotificationRequest;
import com.team11.insurance.notifications.NotificationServiceStub;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.policies.PolicyStatus;
import com.team11.insurance.policies.PolicyView;
import com.team11.insurance.policies.StubPolicyQuery;

/**
 * Integration tests for {@link UnderwritingEngine} verifying end-to-end evaluation with
 * database-seeded plans, missing configuration handling (TC-UW-03), and manual underwriter decisions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnderwritingEngineIntegrationTest {

    @Autowired
    private IUnderwriting underwriting;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationHistoryRepository historyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private INotify notify;

    @Autowired
    private IPolicyQuery policyQuery;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        if (notify instanceof NotificationServiceStub stub) {
            stub.clear();
        }
        ensurePlan("PLAN-A-M1", "MOTOR_FOUR_WHEELER", 1L,
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15,\"rejectIfActivePolicy\":true}");
        ensurePlan("PLAN-A-H1", "HEALTH", 1L,
                "{\"maxAge\":70,\"referralAge\":60,\"preExistingReferral\":true}");
        ensurePlan("PLAN-A-H3", "HEALTH", 1L, null);
    }

    private Plan ensurePlan(String code, String type, Long insurerId, String uwRulesJson) {
        return planRepository.findByCode(code).orElseGet(() ->
                planRepository.save(new Plan(
                        code, insurerId, type, code + " Test Plan",
                        new BigDecimal("500000.00"), BigDecimal.ZERO, 0, BigDecimal.ZERO,
                        "{}", uwRulesJson, "[]", true
                ))
        );
    }

    private Plan findPlanByCode(String code) {
        return planRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Required test plan not found: " + code));
    }

    @Test
    @DisplayName("Evaluate motor application against seeded PLAN-A-M1 rules")
    void evaluateMotorApplicationSeededPlan() {
        Plan plan = findPlanByCode("PLAN-A-M1");

        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(plan.getInsurerId());
        app.setPlanId(plan.getId());
        app.setQuoteId(2001L);
        app.setProductType(plan.getType());
        app.setVehicleRegNo("KA02MN7777");
        app.setPremium(new BigDecimal("5000.00"));
        app.setDetailsJson("{\"regYear\":2020,\"vehicleType\":\"4W\"}");

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.APPROVE);
    }

    @Test
    @DisplayName("Evaluate motor application with duplicate active policy on seeded plan results in REJECT")
    void evaluateMotorApplicationActivePolicyReject() {
        Plan plan = findPlanByCode("PLAN-A-M1");

        // Seed an active policy into the stub policy query for this vehicle
        if (policyQuery instanceof StubPolicyQuery stub) {
            stub.add(new PolicyView(
                    9901L, "POL-2026-ACTIVE1", 1001L, plan.getInsurerId(), plan.getId(),
                    "KA02MN8888", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), PolicyStatus.ACTIVE
            ));
        }

        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(plan.getInsurerId());
        app.setPlanId(plan.getId());
        app.setQuoteId(2002L);
        app.setProductType(plan.getType());
        app.setVehicleRegNo("KA02MN8888");
        app.setPremium(new BigDecimal("5000.00"));
        app.setDetailsJson("{\"regYear\":2021,\"vehicleType\":\"4W\"}"); // Age ~5 years, would otherwise approve

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("active policy");
    }

    @Test
    @DisplayName("Evaluate health application against seeded PLAN-A-H1 rules (age 35 -> Approve)")
    void evaluateHealthApplicationSeededPlanApprove() {
        Plan plan = findPlanByCode("PLAN-A-H1");

        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(plan.getInsurerId());
        app.setPlanId(plan.getId());
        app.setQuoteId(2003L);
        app.setProductType("HEALTH");
        app.setPremium(new BigDecimal("8500.00"));
        app.setDetailsJson("{\"members\":[{\"age\":35}],\"preExisting\":false}");

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.APPROVE);
    }

    @Test
    @DisplayName("Evaluate health application with pre-existing disease yields REFER")
    void evaluateHealthApplicationWithPreExistingDisease() {
        Plan plan = findPlanByCode("PLAN-A-H1");

        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(plan.getInsurerId());
        app.setPlanId(plan.getId());
        app.setQuoteId(2004L);
        app.setProductType("HEALTH");
        app.setPremium(new BigDecimal("8500.00"));
        app.setDetailsJson("{\"members\":[{\"age\":35}],\"preExisting\":true}");

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).containsIgnoringCase("pre-existing");
    }

    @Test
    @DisplayName("TC-UW-03: Plan PLAN-A-H3 with unconfigured underwriting rules yields REFER with exact reason")
    void evaluatePlanWithUnconfiguredRulesYieldsRefer() {
        Plan plan = findPlanByCode("PLAN-A-H3");
        assertThat(plan.getUnderwritingRulesJson()).isNull();

        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(plan.getInsurerId());
        app.setPlanId(plan.getId());
        app.setQuoteId(2005L);
        app.setProductType("HEALTH");
        app.setPremium(new BigDecimal("7500.00"));
        app.setDetailsJson("{\"members\":[{\"age\":28}],\"preExisting\":false}");

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).isEqualTo("Missing rule configuration");
    }

    @Test
    @DisplayName("TC-UW-03: Application with non-existent plan ID yields REFER with exact reason")
    void evaluateWithNonExistentPlanYieldsRefer() {
        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(1L);
        app.setPlanId(999999L); // Non-existent plan ID
        app.setQuoteId(2006L);
        app.setProductType("HEALTH");
        app.setDetailsJson("{\"age\":25}");

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).isEqualTo("Missing rule configuration");
    }

    @Test
    @DisplayName("TC-UW-03: Application with null plan ID yields REFER with exact reason")
    void evaluateWithNullPlanIdYieldsRefer() {
        Application app = new Application();
        app.setPlanId(null);

        UnderwritingResult result = underwriting.evaluate(app);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).isEqualTo("Missing rule configuration");
    }

    @Test
    @DisplayName("Record manual underwriter APPROVAL transitions state, records history and audit log")
    void recordManualApproval() {
        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(1L);
        app.setPlanId(1L);
        app.setQuoteId(3001L);
        app.setProductType("HEALTH");
        app.setPremium(new BigDecimal("8500.00"));
        app.setStatus(ApplicationStatus.REFERRED);
        app = applicationRepository.saveAndFlush(app);

        underwriting.recordManualDecision(app.getId(), Decision.APPROVE, "Medical records verified", 501L);

        Application updated = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        List<ApplicationHistory> history = historyRepository.findByApplicationIdOrderByIdAsc(app.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(history.get(0).getToStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(history.get(0).getActorId()).isEqualTo(501L);
        assertThat(history.get(0).getReason()).isEqualTo("Medical records verified");

        List<AuditLog> auditLogs = auditLogRepository.findByEntityAndEntityId("Application", app.getId());
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getAction()).isEqualTo("UNDERWRITING_MANUAL_APPROVE");
        assertThat(auditLogs.get(0).getUserId()).isEqualTo(501L);

        // Verify notification was sent once to applicant
        if (notify instanceof NotificationServiceStub stub) {
            List<NotificationRequest> approvedNotifs = stub.getNotificationsForEvent(NotificationEvent.APPLICATION_APPROVED);
            assertThat(approvedNotifs).hasSize(1);
            assertThat(approvedNotifs.get(0).recipientUserId()).isEqualTo(1001L);
            assertThat(approvedNotifs.get(0).params().get("applicationId")).isEqualTo(String.valueOf(app.getId()));
            assertThat(approvedNotifs.get(0).params().get("decision")).isEqualTo("APPROVE");
        }
    }

    @Test
    @DisplayName("Record manual underwriter REJECTION sets rejectionReason and records history, audit and notification")
    void recordManualRejection() {
        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(1L);
        app.setPlanId(1L);
        app.setQuoteId(3002L);
        app.setProductType("MOTOR_FOUR_WHEELER");
        app.setPremium(new BigDecimal("6000.00"));
        app.setStatus(ApplicationStatus.REFERRED);
        app = applicationRepository.saveAndFlush(app);

        underwriting.recordManualDecision(app.getId(), Decision.REJECT, "High claims frequency in past year", 501L);

        Application updated = applicationRepository.findById(app.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(updated.getRejectionReason()).isEqualTo("High claims frequency in past year");

        List<ApplicationHistory> history = historyRepository.findByApplicationIdOrderByIdAsc(app.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isEqualTo(ApplicationStatus.REFERRED);
        assertThat(history.get(0).getToStatus()).isEqualTo(ApplicationStatus.REJECTED);

        List<AuditLog> auditLogs = auditLogRepository.findByEntityAndEntityId("Application", app.getId());
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getAction()).isEqualTo("UNDERWRITING_MANUAL_REJECT");
        assertThat(auditLogs.get(0).getUserId()).isEqualTo(501L);

        // Verify notification was sent once to applicant
        if (notify instanceof NotificationServiceStub stub) {
            List<NotificationRequest> rejectedNotifs = stub.getNotificationsForEvent(NotificationEvent.APPLICATION_REJECTED);
            assertThat(rejectedNotifs).hasSize(1);
            assertThat(rejectedNotifs.get(0).recipientUserId()).isEqualTo(1001L);
            assertThat(rejectedNotifs.get(0).params().get("applicationId")).isEqualTo(String.valueOf(app.getId()));
            assertThat(rejectedNotifs.get(0).params().get("decision")).isEqualTo("REJECT");
            assertThat(rejectedNotifs.get(0).params().get("reason")).isEqualTo("High claims frequency in past year");
        }
    }

    @Test
    @DisplayName("Recording manual decision on already APPROVED application throws StateConflictException")
    void invalidStateTransitionThrowsException() {
        Application app = new Application();
        app.setCustomerId(1001L);
        app.setInsurerId(1L);
        app.setPlanId(1L);
        app.setQuoteId(3003L);
        app.setProductType("HEALTH");
        app.setPremium(new BigDecimal("8500.00"));
        app.setStatus(ApplicationStatus.APPROVED); // Already approved!
        app = applicationRepository.saveAndFlush(app);

        final long appId = app.getId();
        assertThatThrownBy(() -> underwriting.recordManualDecision(appId, Decision.REJECT, "Too late", 501L))
                .isInstanceOf(StateConflictException.class);
    }

    @Test
    @DisplayName("Recording decision on non-existent application throws ResourceNotFoundException")
    void nonExistentApplicationThrowsException() {
        assertThatThrownBy(() -> underwriting.recordManualDecision(999999L, Decision.APPROVE, "Test", 501L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
