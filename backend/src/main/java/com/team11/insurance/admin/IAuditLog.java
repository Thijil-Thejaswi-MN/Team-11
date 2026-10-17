package com.team11.insurance.admin;

/**
 * Internal interface for immutable, append-only audit logging (C11 - SAD Sections 3.4 & 4.3.7,
 * Requirements INS-F-018 & INS-SR-008).
 * <p>
 * Used across modules to record security- and workflow-relevant actions with server-assigned timestamps.
 */
public interface IAuditLog {

    /**
     * Sentinel actor ID representing automated, system-generated background operations
     * (e.g., rule-based auto-underwriting, scheduled renewal reminders, expired claim timeouts)
     * where no interactive human user initiated the action.
     * <p>
     * Preserves the {@code NOT NULL} database column constraint on {@code audit_logs.user_id}
     * without inventing a fictional user account or colliding with positive surrogate keys
     * assigned to real users (INS-SR-008, SAD §4.3.7).
     */
    long SYSTEM_USER_ID = 0L;

    /**
     * Records an audit event with standard parameters.
     *
     * @param userId the ID of the acting user (or {@link #SYSTEM_USER_ID} for system actions)
     * @param action the security or workflow action performed (e.g., LOGIN, POLICY_ISSUED)
     * @param entity the domain entity type (e.g., Application, Policy, Claim)
     * @param entityId the ID of the affected domain entity
     */
    void record(long userId, String action, String entity, long entityId);

    /**
     * Records an audit event with additional details or payload text.
     *
     * @param userId the ID of the acting user (or {@link #SYSTEM_USER_ID} for system actions)
     * @param action the security or workflow action performed
     * @param entity the domain entity type
     * @param entityId the ID of the affected domain entity
     * @param details contextual details or JSON metadata
     */
    void record(long userId, String action, String entity, long entityId, String details);

    /**
     * Records an automated system-generated audit event using {@link #SYSTEM_USER_ID}.
     *
     * @param action the automated workflow action performed
     * @param entity the domain entity type
     * @param entityId the ID of the affected domain entity
     */
    default void recordSystem(String action, String entity, long entityId) {
        record(SYSTEM_USER_ID, action, entity, entityId, null);
    }

    /**
     * Records an automated system-generated audit event with details using {@link #SYSTEM_USER_ID}.
     *
     * @param action the automated workflow action performed
     * @param entity the domain entity type
     * @param entityId the ID of the affected domain entity
     * @param details contextual details or JSON metadata
     */
    default void recordSystem(String action, String entity, long entityId, String details) {
        record(SYSTEM_USER_ID, action, entity, entityId, details);
    }
}
