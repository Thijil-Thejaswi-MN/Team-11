package com.team11.insurance.admin;

/**
 * Internal interface for immutable, append-only audit logging (C11 - SAD Sections 3.4 & 4.3.7,
 * Requirements INS-F-018 & INS-SR-008).
 * <p>
 * Used across modules to record security- and workflow-relevant actions with server-assigned timestamps.
 */
public interface IAuditLog {

    /**
     * Records an audit event with standard parameters.
     *
     * @param userId the ID of the acting user
     * @param action the security or workflow action performed (e.g., LOGIN, POLICY_ISSUED)
     * @param entity the domain entity type (e.g., Application, Policy, Claim)
     * @param entityId the ID of the affected domain entity
     */
    void record(long userId, String action, String entity, long entityId);

    /**
     * Records an audit event with additional details or payload text.
     *
     * @param userId the ID of the acting user
     * @param action the security or workflow action performed
     * @param entity the domain entity type
     * @param entityId the ID of the affected domain entity
     * @param details contextual details or JSON metadata
     */
    void record(long userId, String action, String entity, long entityId, String details);
}
