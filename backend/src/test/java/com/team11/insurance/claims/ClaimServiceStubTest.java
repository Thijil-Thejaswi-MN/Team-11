package com.team11.insurance.claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-06 ClaimServiceStub Unit Tests")
class ClaimServiceStubTest {

    private ClaimServiceStub claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimServiceStub();
    }

    @Test
    @DisplayName("hasPaidClaim() returns false by default for any policyId in P4-06 stub baseline")
    void testHasPaidClaimDefaultFalse() {
        assertFalse(claimService.hasPaidClaim(101L));
        assertFalse(claimService.hasPaidClaim(202L));
        assertFalse(claimService.hasPaidClaim(null));
    }

    @Test
    @DisplayName("setHasPaidClaim() configures synthetic claim settlements for test scenarios")
    void testSyntheticHasPaidClaimOverride() {
        Long policyWithSettlement = 303L;
        Long policyWithoutSettlement = 404L;

        claimService.setHasPaidClaim(policyWithSettlement, true);

        assertTrue(claimService.hasPaidClaim(policyWithSettlement),
                "Policy with simulated settled claim must return true");
        assertFalse(claimService.hasPaidClaim(policyWithoutSettlement),
                "Unconfigured policy must continue returning false");
    }

    @Test
    @DisplayName("reset() restores baseline false state across all policies")
    void testReset() {
        claimService.setHasPaidClaim(505L, true);
        assertTrue(claimService.hasPaidClaim(505L));

        claimService.reset();
        assertFalse(claimService.hasPaidClaim(505L));
    }
}
