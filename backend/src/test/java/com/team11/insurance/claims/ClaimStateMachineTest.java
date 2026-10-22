package com.team11.insurance.claims;

import static com.team11.insurance.claims.ClaimStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.team11.insurance.common.exception.StateConflictException;

/**
 * Focused unit tests for canonical claim state machine transitions (SAD §4.1.3, SRS §4.7.1, ADR-008).
 */
class ClaimStateMachineTest {

    @Test
    @DisplayName("Submitted can only transition to Document Verification")
    void submittedTransitions() {
        assertTrue(ClaimStateMachine.canTransition(SUBMITTED, DOCUMENT_VERIFICATION));

        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, ASSESSMENT));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, APPROVED));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, PAYMENT_PROCESSING));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, SETTLED));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, REJECTED));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, CLOSED_NO_RESPONSE));
    }

    @Test
    @DisplayName("Document Verification permitted transitions")
    void documentVerificationTransitions() {
        // Document requests or re-uploads keep the claim in Document Verification
        assertTrue(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, DOCUMENT_VERIFICATION));
        // Verification complete moves to Assessment
        assertTrue(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, ASSESSMENT));
        // Direct rejection by officer
        assertTrue(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, REJECTED));
        // Inactivity timeout of 15 days moves to Closed - No Response
        assertTrue(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, CLOSED_NO_RESPONSE));

        // Illegal transitions from Document Verification
        assertFalse(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, SUBMITTED));
        assertFalse(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, APPROVED));
        assertFalse(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, PAYMENT_PROCESSING));
        assertFalse(ClaimStateMachine.canTransition(DOCUMENT_VERIFICATION, SETTLED));
    }

    @Test
    @DisplayName("Assessment permitted transitions")
    void assessmentTransitions() {
        // Approval transitions directly to Payment Processing (SAD §4.1.3, §4.3.5)
        assertTrue(ClaimStateMachine.canTransition(ASSESSMENT, PAYMENT_PROCESSING));
        // Officer rejection
        assertTrue(ClaimStateMachine.canTransition(ASSESSMENT, REJECTED));

        // Illegal transitions from Assessment
        assertFalse(ClaimStateMachine.canTransition(ASSESSMENT, SUBMITTED));
        assertFalse(ClaimStateMachine.canTransition(ASSESSMENT, DOCUMENT_VERIFICATION));
        assertFalse(ClaimStateMachine.canTransition(ASSESSMENT, SETTLED));
        assertFalse(ClaimStateMachine.canTransition(ASSESSMENT, CLOSED_NO_RESPONSE));
    }

    @Test
    @DisplayName("Payment Processing permitted transitions")
    void paymentProcessingTransitions() {
        // Payout retry keeps in Payment Processing
        assertTrue(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, PAYMENT_PROCESSING));
        // Successful payout moves to Settled
        assertTrue(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, SETTLED));

        // Illegal transitions from Payment Processing
        assertFalse(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, SUBMITTED));
        assertFalse(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, DOCUMENT_VERIFICATION));
        assertFalse(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, ASSESSMENT));
        assertFalse(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, REJECTED));
        assertFalse(ClaimStateMachine.canTransition(PAYMENT_PROCESSING, CLOSED_NO_RESPONSE));
    }

    @ParameterizedTest
    @EnumSource(ClaimStatus.class)
    @DisplayName("Terminal state SETTLED has no outbound transitions")
    void settledIsTerminal(ClaimStatus target) {
        assertTrue(ClaimStateMachine.isFinal(SETTLED));
        assertFalse(ClaimStateMachine.canTransition(SETTLED, target));
        assertThrows(StateConflictException.class, () -> ClaimStateMachine.requireTransition(SETTLED, target));
    }

    @ParameterizedTest
    @EnumSource(ClaimStatus.class)
    @DisplayName("Terminal state REJECTED has no outbound transitions")
    void rejectedIsTerminal(ClaimStatus target) {
        assertTrue(ClaimStateMachine.isFinal(REJECTED));
        assertFalse(ClaimStateMachine.canTransition(REJECTED, target));
        assertThrows(StateConflictException.class, () -> ClaimStateMachine.requireTransition(REJECTED, target));
    }

    @ParameterizedTest
    @EnumSource(ClaimStatus.class)
    @DisplayName("Terminal state CLOSED_NO_RESPONSE has no outbound transitions")
    void closedNoResponseIsTerminal(ClaimStatus target) {
        assertTrue(ClaimStateMachine.isFinal(CLOSED_NO_RESPONSE));
        assertFalse(ClaimStateMachine.canTransition(CLOSED_NO_RESPONSE, target));
        assertThrows(StateConflictException.class, () -> ClaimStateMachine.requireTransition(CLOSED_NO_RESPONSE, target));
    }

    @Test
    @DisplayName("requireTransition throws StateConflictException on invalid transition")
    void requireTransitionThrowsOnConflict() {
        StateConflictException ex = assertThrows(StateConflictException.class,
                () -> ClaimStateMachine.requireTransition(SUBMITTED, SETTLED));
        assertTrue(ex.getMessage().contains("cannot transition from SUBMITTED to SETTLED"));
    }

    @Test
    @DisplayName("requireTransition succeeds on valid transition")
    void requireTransitionSucceedsOnValid() {
        assertDoesNotThrow(() -> ClaimStateMachine.requireTransition(SUBMITTED, DOCUMENT_VERIFICATION));
        assertDoesNotThrow(() -> ClaimStateMachine.requireTransition(DOCUMENT_VERIFICATION, ASSESSMENT));
        assertDoesNotThrow(() -> ClaimStateMachine.requireTransition(ASSESSMENT, PAYMENT_PROCESSING));
        assertDoesNotThrow(() -> ClaimStateMachine.requireTransition(PAYMENT_PROCESSING, SETTLED));
    }

    @Test
    @DisplayName("Null handling in transitions")
    void nullHandling() {
        assertFalse(ClaimStateMachine.canTransition(null, SUBMITTED));
        assertFalse(ClaimStateMachine.canTransition(SUBMITTED, null));
        assertFalse(ClaimStateMachine.canTransition(null, null));
        assertFalse(ClaimStateMachine.isFinal(null));
        assertTrue(ClaimStateMachine.getAllowedNextStates(null).isEmpty());

        assertThrows(StateConflictException.class, () -> ClaimStateMachine.requireTransition(null, SUBMITTED));
        assertThrows(StateConflictException.class, () -> ClaimStateMachine.requireTransition(SUBMITTED, null));
    }
}
