package com.team11.insurance.applications;

import static com.team11.insurance.applications.ApplicationStatus.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.team11.insurance.common.exception.StateConflictException;

/**
 * The only place that lists allowed application transitions (SAD ADR-008).
 * Pure Java, so it is unit-tested without Spring or a database.
 *
 * The service must call requireTransition(...), then change the status AND insert an
 * ApplicationHistory row in the same transaction. A violation throws StateConflictException,
 * which the GlobalExceptionHandler turns into 409 STATE-409.
 */
public final class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED =
            new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(SUBMITTED, EnumSet.of(APPROVED, REFERRED, REJECTED)); // underwriting result
        ALLOWED.put(REFERRED,  EnumSet.of(APPROVED, REJECTED));           // underwriter decision
        ALLOWED.put(APPROVED,  EnumSet.of(PAID));                         // payment success
        ALLOWED.put(REJECTED,  EnumSet.noneOf(ApplicationStatus.class));  // final
        ALLOWED.put(PAID,      EnumSet.noneOf(ApplicationStatus.class));  // final
    }

    private ApplicationStateMachine() { }

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    public static void requireTransition(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new StateConflictException("Application cannot move from " + from + " to " + to);
        }
    }

    public static boolean isFinal(ApplicationStatus status) {
        return ALLOWED.get(status).isEmpty();
    }
}
