package com.team11.insurance.claims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.common.time.MutableTestClock;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;

/**
 * Integration test suite for ClaimAutoCloseScheduler (Task D, P1-19, TC-Clm-06, SAD §4.1.3, §4.3.5).
 * Tests virtual clock progression:
 * - Documents requested on day 0 -> Still open in Document Verification on day 14
 * - On day 16 (>15 days inactivity) -> Transitions to Closed - No Response
 * - CLAIM_CLOSED notification emitted exactly once
 * - Second execution closes nothing new (idempotency)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ClaimAutoCloseScheduler Integration Tests (P1-19, TC-Clm-06)")
class ClaimAutoCloseSchedulerIntegrationTest {

    @Autowired
    private ClaimAutoCloseScheduler scheduler;

    @Autowired
    private MutableTestClock testClock;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimHistoryRepository claimHistoryRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @MockBean
    private INotify notify;

    private Policy testPolicy;

    @BeforeEach
    void setUp() {
        testPolicy = new Policy();
        testPolicy.setPolicyNo("POL-AUTOCLOSE-01");
        testPolicy.setCustomerId(1001L);
        testPolicy.setInsurerId(1L);
        testPolicy.setPlanId(1L);
        testPolicy.setPremiumPaid(new BigDecimal("15000.00"));
        testPolicy.setStartDate(LocalDate.of(2026, 1, 1));
        testPolicy.setEndDate(LocalDate.of(2027, 1, 1));
        testPolicy.setStatus(PolicyStatus.ACTIVE);
        testPolicy = policyRepository.saveAndFlush(testPolicy);
    }

    @AfterEach
    void tearDown() {
        testClock.reset();
    }

    @Test
    @DisplayName("TC-Clm-06: Day 0 doc request -> Open on day 14, Closed on day 16, CLAIM_CLOSED emitted once, idempotent second run")
    void autoClose_day0Request_day14Open_day16Closed_emitsNotificationOnce_secondRunIdempotent() {
        // Day 0: Initial document request recorded
        Instant day0 = Instant.parse("2026-10-01T10:00:00Z");
        testClock.setFixed(day0);

        Claim claim = new Claim();
        claim.setClaimNo("CLM-2026-AUTO-001");
        claim.setPolicyId(testPolicy.getId());
        claim.setPolicyNo(testPolicy.getPolicyNo());
        claim.setCustomerId(1001L);
        claim.setInsurerId(1L);
        claim.setIncidentDate(LocalDate.of(2026, 9, 25));
        claim.setClaimAmount(new BigDecimal("20000.00"));
        claim.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);
        claim.setDocumentRequestedAt(day0);
        claim = claimRepository.saveAndFlush(claim);

        // Day 14: Customer has not replied. Run daily scheduler.
        Instant day14 = day0.plus(14, ChronoUnit.DAYS);
        testClock.setFixed(day14);

        int closedOnDay14 = scheduler.runAutoCloseJob();
        assertEquals(0, closedOnDay14, "On day 14, claim must remain open within the 15-day grace period");

        Claim claimDay14 = claimRepository.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, claimDay14.getStatus(), "Claim must remain in Document Verification on day 14");
        verify(notify, never()).send(eq(NotificationEvent.CLAIM_CLOSED), anyLong(), any());

        // Day 16: Inactivity exceeds 15-day window. Run daily scheduler.
        Instant day16 = day0.plus(16, ChronoUnit.DAYS);
        testClock.setFixed(day16);

        int closedOnDay16 = scheduler.runAutoCloseJob();
        assertEquals(1, closedOnDay16, "On day 16, claim must transition to Closed - No Response");

        Claim claimDay16 = claimRepository.findById(claim.getId()).orElseThrow();
        assertEquals(ClaimStatus.CLOSED_NO_RESPONSE, claimDay16.getStatus(), "Claim must be in Closed - No Response on day 16");
        assertNotNull(claimDay16.getReason());

        // CLAIM_CLOSED notification emitted once to customer 1001
        verify(notify, times(1)).send(
                eq(NotificationEvent.CLAIM_CLOSED),
                eq(1001L),
                argThat(params -> params.containsKey("claimNo") && params.get("claimNo").equals("CLM-2026-AUTO-001"))
        );

        // Verify history record logged transition
        List<ClaimHistory> histories = claimHistoryRepository.findByClaimIdOrderByIdAsc(claim.getId());
        boolean hasClosedHistory = histories.stream()
                .anyMatch(h -> h.getToStatus() == ClaimStatus.CLOSED_NO_RESPONSE);
        assertEquals(true, hasClosedHistory, "ClaimHistory must record transition to Closed - No Response");

        // Second run on Day 16: Idempotent sweep closes nothing new
        int secondRunClosed = scheduler.runAutoCloseJob();
        assertEquals(0, secondRunClosed, "Subsequent scheduler run must close nothing new");

        // CLAIM_CLOSED notification is NOT sent again (strictly once)
        verify(notify, times(1)).send(eq(NotificationEvent.CLAIM_CLOSED), eq(1001L), any());
    }

    @Test
    @DisplayName("autoClose ignores claims in other lifecycle states (Assessment / Payment Processing)")
    void autoClose_otherLifecycleStates_ignored() {
        Instant day0 = Instant.parse("2026-10-01T10:00:00Z");
        testClock.setFixed(day0);

        Claim assessmentClaim = new Claim();
        assessmentClaim.setClaimNo("CLM-2026-ASSESS-001");
        assessmentClaim.setPolicyId(testPolicy.getId());
        assessmentClaim.setPolicyNo(testPolicy.getPolicyNo());
        assessmentClaim.setCustomerId(1001L);
        assessmentClaim.setInsurerId(1L);
        assessmentClaim.setIncidentDate(LocalDate.of(2026, 9, 20));
        assessmentClaim.setClaimAmount(new BigDecimal("18000.00"));
        assessmentClaim.setStatus(ClaimStatus.ASSESSMENT);
        assessmentClaim.setDocumentRequestedAt(day0);
        claimRepository.saveAndFlush(assessmentClaim);

        // Advance 20 days
        testClock.setFixed(day0.plus(20, ChronoUnit.DAYS));

        int closed = scheduler.runAutoCloseJob();
        assertEquals(0, closed, "Claims in Assessment state must not be closed by auto-close sweep");

        Claim after = claimRepository.findById(assessmentClaim.getId()).orElseThrow();
        assertEquals(ClaimStatus.ASSESSMENT, after.getStatus());
    }
}
