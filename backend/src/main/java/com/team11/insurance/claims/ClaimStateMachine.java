package com.team11.insurance.claims;

import static com.team11.insurance.claims.ClaimStatus.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.team11.insurance.common.exception.StateConflictException;

/**
 * Authoritative Claim State Machine (SAD §4.1.3, SRS §4.7.1, ADR-008, P4-03 Contract §5.2).
 * Pure Java, unit-testable without Spring context.
 * Invalid transitions throw StateConflictException, mapped to HTTP 409 STATE-409.
 */
public final class ClaimStateMachine {

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = new EnumMap<>(ClaimStatus.class);

    static {
        // Submitted -> Document Verification (Immediate intake & duplicate check)
        ALLOWED.put(SUBMITTED, EnumSet.of(DOCUMENT_VERIFICATION));

        // Document Verification -> Document Verification (doc request/re-upload), Assessment, Rejected, Closed - No Response
        ALLOWED.put(DOCUMENT_VERIFICATION, EnumSet.of(DOCUMENT_VERIFICATION, ASSESSMENT, REJECTED, CLOSED_NO_RESPONSE));

        // Assessment -> Payment Processing, Rejected
        ALLOWED.put(ASSESSMENT, EnumSet.of(PAYMENT_PROCESSING, REJECTED));

        // Approved -> Payment Processing (compatibility bridge)
        ALLOWED.put(APPROVED, EnumSet.of(PAYMENT_PROCESSING));

        // Payment Processing -> Payment Processing (retry), Settled
        ALLOWED.put(PAYMENT_PROCESSING, EnumSet.of(PAYMENT_PROCESSING, SETTLED));

        // Terminal states
        ALLOWED.put(SETTLED, EnumSet.noneOf(ClaimStatus.class));
        ALLOWED.put(REJECTED, EnumSet.noneOf(ClaimStatus.class));
        ALLOWED.put(CLOSED_NO_RESPONSE, EnumSet.noneOf(ClaimStatus.class));
    }

    private ClaimStateMachine() {
    }

    /**
     * Checks if a transition from one status to another is permitted.
     *
     * @param from current claim status
     * @param to proposed target status
     * @return true if permitted; false otherwise
     */
    public static boolean canTransition(ClaimStatus from, ClaimStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<ClaimStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * Validates and enforces a state transition, throwing StateConflictException (HTTP 409) if illegal.
     *
     * @param from current claim status
     * @param to proposed target status
     * @throws StateConflictException if transition is not permitted
     */
    public static void requireTransition(ClaimStatus from, ClaimStatus to) {
        if (!canTransition(from, to)) {
            throw new StateConflictException("Claim cannot transition from " + from + " to " + to);
        }
    }

    /**
     * Checks whether the status represents a terminal state with no outbound transitions.
     *
     * @param status status to check
     * @return true if terminal; false otherwise
     */
    public static boolean isFinal(ClaimStatus status) {
        if (status == null) {
            return false;
        }
        Set<ClaimStatus> targets = ALLOWED.get(status);
        return targets == null || targets.isEmpty();
    }

    /**
     * Returns an unmodifiable set of allowed next states from the given status.
     *
     * @param from current status
     * @return set of allowed next states
     */
    public static Set<ClaimStatus> getAllowedNextStates(ClaimStatus from) {
        if (from == null) {
            return Collections.emptySet();
        }
        Set<ClaimStatus> targets = ALLOWED.get(from);
        return targets != null ? Collections.unmodifiableSet(targets) : Collections.emptySet();
    }
}
