package com.team11.insurance.claims;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;
import com.team11.insurance.policies.PolicyView;

/**
 * Focused tests for ClaimService:
 * - Settled-only hasPaidClaim behavior (Decision D-09.5)
 * - Policy validation (active/inactive/missing)
 * - 30-day incident boundary and policy coverage period
 * - Ownership and multi-tenant insurer authorization (fail closed)
 * - CLAIM_RECEIVED notification emission
 * - Duplicate detection flag (UC-24)
 */
class ClaimServiceTest {

    private ClaimRepository claimRepository;
    private ClaimHistoryRepository claimHistoryRepository;
    private IPolicyQuery policyQuery;
    private INotify notify;
    private Clock fixedClock;
    private ClaimService claimService;

    // Fixed date: 2026-10-15
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 10, 15);
    private static final Instant FIXED_INSTANT = FIXED_TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant();

    private final CurrentUser customerOwner = new CurrentUser(1001L, UserRole.CUSTOMER, null);
    private final CurrentUser otherCustomer = new CurrentUser(1002L, UserRole.CUSTOMER, null);
    private final CurrentUser claimsOfficerInsurer1 = new CurrentUser(2001L, UserRole.CLAIMS_OFFICER, 1L);
    private final CurrentUser claimsOfficerInsurer2 = new CurrentUser(2002L, UserRole.CLAIMS_OFFICER, 2L);

    @BeforeEach
    void setUp() {
        claimRepository = mock(ClaimRepository.class);
        claimHistoryRepository = mock(ClaimHistoryRepository.class);
        policyQuery = mock(IPolicyQuery.class);
        notify = mock(INotify.class);
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

        claimService = new ClaimService(
                claimRepository,
                claimHistoryRepository,
                policyQuery,
                null,
                notify,
                fixedClock
        );
    }

    private PolicyView activePolicy(Long id, Long customerId, Long insurerId) {
        return new PolicyView(
                id,
                "POL-2026-000101",
                customerId,
                insurerId,
                10L,
                "KA01AB1234",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                PolicyStatus.ACTIVE
        );
    }

    // =========================================================================
    // 1. hasPaidClaim tests (Decision D-09.5: SETTLED only)
    // =========================================================================

    @Test
    @DisplayName("hasPaidClaim returns true only when claim has status SETTLED")
    void hasPaidClaim_settledOnly() {
        when(claimRepository.existsByPolicyIdAndStatus(100L, ClaimStatus.SETTLED)).thenReturn(true);
        when(claimRepository.existsByPolicyIdAndStatus(101L, ClaimStatus.SETTLED)).thenReturn(false);

        assertTrue(claimService.hasPaidClaim(100L));
        assertFalse(claimService.hasPaidClaim(101L));
        assertFalse(claimService.hasPaidClaim(null));

        verify(claimRepository).existsByPolicyIdAndStatus(100L, ClaimStatus.SETTLED);
        verify(claimRepository).existsByPolicyIdAndStatus(101L, ClaimStatus.SETTLED);
        verify(claimRepository, never()).existsByPolicyIdAndStatus(any(), eq(ClaimStatus.APPROVED));
        verify(claimRepository, never()).existsByPolicyIdAndStatus(any(), eq(ClaimStatus.PAYMENT_PROCESSING));
    }

    // =========================================================================
    // 2. Policy existence and status validation
    // =========================================================================

    @Test
    @DisplayName("file claim fails when policy does not exist (404)")
    void file_policyNotFound() {
        when(policyQuery.getPolicy(999L)).thenThrow(new ResourceNotFoundException("Policy", 999L));

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                999L, FIXED_TODAY.minusDays(5), BigDecimal.valueOf(5000), "Accident", List.of(1L));

        assertThrows(ResourceNotFoundException.class, () -> claimService.file(req, customerOwner));
    }

    @Test
    @DisplayName("file claim fails when policy is EXPIRED (409 STATE-409)")
    void file_policyExpired_throwsStateConflict() {
        PolicyView expired = new PolicyView(
                100L, "POL-EXP", customerOwner.getUserId(), 1L, 10L, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), PolicyStatus.EXPIRED);
        when(policyQuery.getPolicy(100L)).thenReturn(expired);

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY.minusDays(5), BigDecimal.valueOf(5000), "Dent", List.of());

        StateConflictException ex = assertThrows(StateConflictException.class, () -> claimService.file(req, customerOwner));
        assertTrue(ex.getMessage().contains("Policy is not active"));
    }

    @Test
    @DisplayName("file claim fails when policy is CANCELLED (409 STATE-409)")
    void file_policyCancelled_throwsStateConflict() {
        PolicyView cancelled = new PolicyView(
                100L, "POL-CAN", customerOwner.getUserId(), 1L, 10L, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), PolicyStatus.CANCELLED);
        when(policyQuery.getPolicy(100L)).thenReturn(cancelled);

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY.minusDays(5), BigDecimal.valueOf(5000), "Dent", List.of());

        assertThrows(StateConflictException.class, () -> claimService.file(req, customerOwner));
    }

    // =========================================================================
    // 3. Ownership and Authentication checks
    // =========================================================================

    @Test
    @DisplayName("file claim fails when currentUser is null (fail closed)")
    void file_nullCurrentUser_throwsAccessDenied() {
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY.minusDays(5), BigDecimal.valueOf(5000), "Dent", List.of());

        assertThrows(AccessDeniedException.class, () -> claimService.file(req, null));
    }

    @Test
    @DisplayName("file claim fails when customer is not the policy owner (403 ACCESS-403)")
    void file_notPolicyOwner_throwsAccessDenied() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY.minusDays(5), BigDecimal.valueOf(5000), "Dent", List.of());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> claimService.file(req, otherCustomer));
        assertTrue(ex.getMessage().contains("does not belong to authenticated customer"));
    }

    // =========================================================================
    // 4. 30-Day Boundary and Policy Coverage Period Tests
    // =========================================================================

    @Test
    @DisplayName("file claim succeeds for incident today (boundary check)")
    void file_incidentToday_succeeds() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));
        when(claimRepository.saveAndFlush(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY, BigDecimal.valueOf(5000), "Bumper dent", List.of(10L));

        ClaimResponse resp = claimService.file(req, customerOwner);

        assertNotNull(resp);
        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, resp.status());
    }

    @Test
    @DisplayName("file claim succeeds for incident exactly 30 days ago (boundary check)")
    void file_incident30DaysAgo_succeeds() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));
        when(claimRepository.saveAndFlush(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            if (c.getId() == null) c.setId(2L);
            return c;
        });

        // 30 days ago: 2026-09-15
        LocalDate incidentDate = FIXED_TODAY.minusDays(30);
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, incidentDate, BigDecimal.valueOf(7500), "Scratch", List.of());

        ClaimResponse resp = claimService.file(req, customerOwner);

        assertNotNull(resp);
        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, resp.status());
    }

    @Test
    @DisplayName("file claim fails when incident is 31 days ago (outside 30-day window, 422)")
    void file_incident31DaysAgo_throwsValidationException() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));

        LocalDate incidentDate = FIXED_TODAY.minusDays(31);
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, incidentDate, BigDecimal.valueOf(7500), "Old dent", List.of());

        ValidationException ex = assertThrows(ValidationException.class, () -> claimService.file(req, customerOwner));
        assertTrue(ex.getMessage().contains("within 30 days"));
    }

    @Test
    @DisplayName("file claim fails when incident date is in the future (422)")
    void file_incidentInFuture_throwsValidationException() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));

        LocalDate future = FIXED_TODAY.plusDays(1);
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, future, BigDecimal.valueOf(7500), "Future dent", List.of());

        ValidationException ex = assertThrows(ValidationException.class, () -> claimService.file(req, customerOwner));
        assertTrue(ex.getMessage().contains("future"));
    }

    @Test
    @DisplayName("file claim fails when incident date is before policy start date (422)")
    void file_incidentBeforePolicyStartDate_throwsValidationException() {
        // Policy starting 2026-10-01
        PolicyView policy = new PolicyView(
                100L, "POL-1", customerOwner.getUserId(), 1L, 10L, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), PolicyStatus.ACTIVE);
        when(policyQuery.getPolicy(100L)).thenReturn(policy);

        LocalDate incidentDate = LocalDate.of(2026, 9, 25); // within 30 days of 10-15, but before policy start
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, incidentDate, BigDecimal.valueOf(5000), "Dent", List.of());

        ValidationException ex = assertThrows(ValidationException.class, () -> claimService.file(req, customerOwner));
        assertTrue(ex.getMessage().contains("outside the policy coverage period"));
    }

    // =========================================================================
    // 5. Notification & History Persistence
    // =========================================================================

    @Test
    @DisplayName("file claim persists SUBMITTED then transitions to DOCUMENT_VERIFICATION and emits CLAIM_RECEIVED")
    void file_emitsClaimReceivedNotificationAndPersistsHistory() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));
        when(claimRepository.saveAndFlush(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10L);
            return c;
        });

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                100L, FIXED_TODAY.minusDays(2), BigDecimal.valueOf(12000), "Headlight crack", List.of(501L));

        ClaimResponse resp = claimService.file(req, customerOwner);

        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, resp.status());
        assertFalse(resp.duplicateFlag());

        // Verify history was saved twice: initial SUBMITTED, then transition to DOCUMENT_VERIFICATION
        ArgumentCaptor<ClaimHistory> historyCaptor = ArgumentCaptor.forClass(ClaimHistory.class);
        verify(claimHistoryRepository, times(2)).save(historyCaptor.capture());
        List<ClaimHistory> histories = historyCaptor.getAllValues();

        assertEquals(ClaimStatus.SUBMITTED, histories.get(0).getToStatus());
        assertNull(histories.get(0).getFromStatus());
        assertEquals(customerOwner.getUserId(), histories.get(0).getActorId());

        assertEquals(ClaimStatus.SUBMITTED, histories.get(1).getFromStatus());
        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, histories.get(1).getToStatus());

        // Verify CLAIM_RECEIVED was emitted
        verify(notify).send(eq(NotificationEvent.CLAIM_RECEIVED), eq(customerOwner.getUserId()), argThat(params ->
                params.containsKey("claimNo") && params.containsKey("policyNo") && "12000".equals(params.get("claimedAmount"))
        ));
    }

    // =========================================================================
    // 6. Duplicate Detection (UC-24)
    // =========================================================================

    @Test
    @DisplayName("file claim with matching policyId, incidentDate, and amount flags duplicate but does not block filing")
    void file_duplicateDetected_flagsDuplicateWithoutBlocking() {
        when(policyQuery.getPolicy(100L)).thenReturn(activePolicy(100L, customerOwner.getUserId(), 1L));
        LocalDate incidentDate = FIXED_TODAY.minusDays(5);
        BigDecimal amount = BigDecimal.valueOf(15000);

        when(claimRepository.existsByPolicyIdAndIncidentDateAndClaimAmount(100L, incidentDate, amount))
                .thenReturn(true);

        when(claimRepository.saveAndFlush(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            if (c.getId() == null) c.setId(11L);
            return c;
        });

        ClaimSubmissionRequest req = new ClaimSubmissionRequest(100L, incidentDate, amount, "Windshield", List.of());
        ClaimResponse resp = claimService.file(req, customerOwner);

        assertTrue(resp.duplicateFlag(), "Duplicate flag must be set to true on match");
        assertEquals(ClaimStatus.DOCUMENT_VERIFICATION, resp.status());
        assertTrue(resp.message().contains("possible duplicate"));
    }

    // =========================================================================
    // 7. GET /claims/{id} Authorization & Multi-tenant Insurer Isolation
    // =========================================================================

    @Test
    @DisplayName("GET claim by customer owner succeeds")
    void get_customerOwner_succeeds() {
        Claim claim = new Claim();
        claim.setId(50L);
        claim.setClaimNo("CLM-2026-000050");
        claim.setPolicyId(100L);
        claim.setCustomerId(customerOwner.getUserId());
        claim.setInsurerId(1L);
        claim.setClaimAmount(BigDecimal.valueOf(8000));
        claim.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));

        ClaimView view = claimService.get(50L, customerOwner);
        assertNotNull(view);
        assertEquals("CLM-2026-000050", view.claimNo());
    }

    @Test
    @DisplayName("GET claim by another customer fails with 403 ACCESS-403")
    void get_otherCustomer_throwsAccessDenied() {
        Claim claim = new Claim();
        claim.setId(50L);
        claim.setCustomerId(customerOwner.getUserId());
        claim.setInsurerId(1L);

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));

        assertThrows(AccessDeniedException.class, () -> claimService.get(50L, otherCustomer));
    }

    @Test
    @DisplayName("GET claim by Claims Officer of matching insurer succeeds")
    void get_matchingInsurerClaimsOfficer_succeeds() {
        Claim claim = new Claim();
        claim.setId(50L);
        claim.setCustomerId(customerOwner.getUserId());
        claim.setInsurerId(1L);

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));

        ClaimView view = claimService.get(50L, claimsOfficerInsurer1);
        assertNotNull(view);
    }

    @Test
    @DisplayName("GET claim by Claims Officer of different insurer fails with 403 (insurer tenant isolation)")
    void get_differentInsurerClaimsOfficer_throwsAccessDenied() {
        Claim claim = new Claim();
        claim.setId(50L);
        claim.setCustomerId(customerOwner.getUserId());
        claim.setInsurerId(1L); // Owned by Insurer 1

        when(claimRepository.findById(50L)).thenReturn(Optional.of(claim));

        // Claims officer belongs to Insurer 2
        assertThrows(AccessDeniedException.class, () -> claimService.get(50L, claimsOfficerInsurer2));
    }

    @Test
    @DisplayName("GET claim fails closed when currentUser is null")
    void get_nullCurrentUser_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> claimService.get(50L, null));
    }

    @Test
    @DisplayName("GET claim fails with 404 when claim is not found")
    void get_notFound_throwsResourceNotFound() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> claimService.get(999L, customerOwner));
    }
}
