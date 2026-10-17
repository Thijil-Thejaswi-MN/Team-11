package com.team11.insurance.underwriting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationStatus;

/**
 * Unit and contract tests for {@link StubUnderwritingEngine} and {@link UnderwritingResult} (P1-05).
 */
class StubUnderwritingEngineTest {

    private final IUnderwriting underwriting = new StubUnderwritingEngine();

    @Test
    void alwaysReturnsApproveWithStubReason() {
        Application application = new Application();
        application.setCustomerId(1L);
        application.setInsurerId(10L);
        application.setPlanId(11L);
        application.setQuoteId(100L);
        application.setProductType("FOUR_WHEELER");
        application.setVehicleRegNo("KA22AB1234");
        application.setPremium(new BigDecimal("9480.00"));
        application.setStatus(ApplicationStatus.SUBMITTED);

        UnderwritingResult result = underwriting.evaluate(application);

        assertNotNull(result);
        assertEquals(UnderwritingOutcome.APPROVE, result.getOutcome());
        assertEquals(StubUnderwritingEngine.STUB_REASON, result.getReason());
        assertTrue(result.isApproved());
        assertFalse(result.isReferred());
        assertFalse(result.isRejected());
    }

    @Test
    void evaluatesNullApplicationSafely() {
        UnderwritingResult result = underwriting.evaluate(null);
        assertNotNull(result);
        assertTrue(result.isApproved());
        assertEquals("TEST_STUB_ALWAYS_APPROVES", result.getReason());
    }

    @Test
    void recordManualDecisionDoesNotThrow() {
        assertDoesNotThrow(() ->
                underwriting.recordManualDecision(7001L, Decision.APPROVE, "Manual review complete", 501L));
        assertDoesNotThrow(() ->
                underwriting.recordManualDecision(7002L, Decision.REJECT, "Failed background verification", 501L));
    }

    @Test
    void underwritingResultFactories() {
        UnderwritingResult approved = UnderwritingResult.approve("Auto-approve rule passed");
        assertTrue(approved.isApproved());
        assertEquals(UnderwritingOutcome.APPROVE, approved.getOutcome());
        assertEquals("Auto-approve rule passed", approved.getReason());

        UnderwritingResult referred = UnderwritingResult.refer("Vehicle age > 15");
        assertTrue(referred.isReferred());
        assertEquals(UnderwritingOutcome.REFER, referred.getOutcome());

        UnderwritingResult rejected = UnderwritingResult.reject("Vehicle age > 20");
        assertTrue(rejected.isRejected());
        assertEquals(UnderwritingOutcome.REJECT, rejected.getOutcome());
    }

    @Test
    void syntheticOutcomeOverridesAlwaysApproveBehavior() {
        try {
            StubUnderwritingEngine.setSyntheticOutcome(UnderwritingResult.refer("Flagged for senior underwriter inspection"));
            UnderwritingResult referred = underwriting.evaluate(new Application());
            assertTrue(referred.isReferred());
            assertEquals("Flagged for senior underwriter inspection", referred.getReason());

            StubUnderwritingEngine.setSyntheticOutcome(UnderwritingResult.reject("High risk postal code"));
            UnderwritingResult rejected = underwriting.evaluate(new Application());
            assertTrue(rejected.isRejected());
            assertEquals("High risk postal code", rejected.getReason());

            StubUnderwritingEngine.clearSyntheticOutcome();
            UnderwritingResult restored = underwriting.evaluate(new Application());
            assertTrue(restored.isApproved());
            assertEquals(StubUnderwritingEngine.STUB_REASON, restored.getReason());
        } finally {
            StubUnderwritingEngine.clearSyntheticOutcome();
        }
    }
}
