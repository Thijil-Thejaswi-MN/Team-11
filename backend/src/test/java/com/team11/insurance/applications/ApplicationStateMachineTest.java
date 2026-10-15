package com.team11.insurance.applications;

import static com.team11.insurance.applications.ApplicationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.StateConflictException;

class ApplicationStateMachineTest {

    @Test
    void submittedCanBecomeApprovedReferredOrRejected() {
        assertTrue(ApplicationStateMachine.canTransition(SUBMITTED, APPROVED));
        assertTrue(ApplicationStateMachine.canTransition(SUBMITTED, REFERRED));
        assertTrue(ApplicationStateMachine.canTransition(SUBMITTED, REJECTED));
    }

    @Test
    void referredCanBecomeApprovedOrRejectedOnly() {
        assertTrue(ApplicationStateMachine.canTransition(REFERRED, APPROVED));
        assertTrue(ApplicationStateMachine.canTransition(REFERRED, REJECTED));
        assertFalse(ApplicationStateMachine.canTransition(REFERRED, PAID));
    }

    @Test
    void approvedCanOnlyBecomePaid() {
        assertTrue(ApplicationStateMachine.canTransition(APPROVED, PAID));
        assertFalse(ApplicationStateMachine.canTransition(APPROVED, REJECTED));
        assertFalse(ApplicationStateMachine.canTransition(APPROVED, SUBMITTED));
    }

    @Test
    void rejectedAndPaidAreFinal() {
        assertTrue(ApplicationStateMachine.isFinal(REJECTED));
        assertTrue(ApplicationStateMachine.isFinal(PAID));
        for (ApplicationStatus to : ApplicationStatus.values()) {
            assertFalse(ApplicationStateMachine.canTransition(REJECTED, to));
            assertFalse(ApplicationStateMachine.canTransition(PAID, to));
        }
    }

    @Test
    void skippingAStateIsBlockedWithState409() {
        StateConflictException ex = assertThrows(StateConflictException.class,
                () -> ApplicationStateMachine.requireTransition(SUBMITTED, PAID));
        assertEquals(ErrorCode.STATE_409, ex.getErrorCode());
    }

    @Test
    void allowedTransitionDoesNotThrow() {
        assertDoesNotThrow(() -> ApplicationStateMachine.requireTransition(APPROVED, PAID));
    }
}
