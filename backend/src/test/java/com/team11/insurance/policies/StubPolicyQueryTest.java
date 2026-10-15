package com.team11.insurance.policies;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.team11.insurance.common.exception.ResourceNotFoundException;

class StubPolicyQueryTest {

    // The test clock stands still at 15 Oct 2026 (test-clock rule).
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC);
    private final StubPolicyQuery query = new StubPolicyQuery(clock);

    private PolicyView policy(long id, String regNo, LocalDate end, PolicyStatus status) {
        return new PolicyView(id, "POL-2026-00000" + id, 1L, 10L, 100L, regNo,
                LocalDate.of(2025, 10, 16), end, status);
    }

    @Test
    void getPolicyReturnsASeededPolicy() {
        query.add(policy(1, "KA22AB1234", LocalDate.of(2027, 1, 1), PolicyStatus.ACTIVE));
        assertEquals("POL-2026-000001", query.getPolicy(1).policyNo());
    }

    @Test
    void getPolicyOfUnknownIdThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> query.getPolicy(99));
    }

    @Test
    void activePolicyOnTheVehicleIsFoundIgnoringCase() {
        query.add(policy(1, "KA22AB1234", LocalDate.of(2027, 1, 1), PolicyStatus.ACTIVE));
        assertTrue(query.existsActivePolicyForVehicle("ka22ab1234"));
    }

    @Test
    void policyEndingTodayStillCounts() {
        query.add(policy(1, "KA22AB1234", LocalDate.of(2026, 10, 15), PolicyStatus.ACTIVE));
        assertTrue(query.existsActivePolicyForVehicle("KA22AB1234"));
    }

    @Test
    void expiredOrCancelledPoliciesDoNotCount() {
        query.add(policy(1, "KA01AA0001", LocalDate.of(2026, 10, 14), PolicyStatus.ACTIVE));
        query.add(policy(2, "KA01AA0002", LocalDate.of(2027, 1, 1), PolicyStatus.CANCELLED));
        query.add(policy(3, "KA01AA0003", LocalDate.of(2027, 1, 1), PolicyStatus.EXPIRED));
        assertFalse(query.existsActivePolicyForVehicle("KA01AA0001"));
        assertFalse(query.existsActivePolicyForVehicle("KA01AA0002"));
        assertFalse(query.existsActivePolicyForVehicle("KA01AA0003"));
    }

    @Test
    void cancellationRequestedStillCountsAsActive() {
        query.add(policy(1, "KA22AB1234", LocalDate.of(2027, 1, 1), PolicyStatus.CANCELLATION_REQUESTED));
        assertTrue(query.existsActivePolicyForVehicle("KA22AB1234"));
    }

    @Test
    void blankOrNullRegistrationIsNeverActive() {
        assertFalse(query.existsActivePolicyForVehicle(null));
        assertFalse(query.existsActivePolicyForVehicle("  "));
    }
}
