package com.team11.insurance.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

/**
 * Append-only Spring Data repository contract for {@link AuditLog} entries (INS-SR-008, SAD §4.3.7).
 * <p>
 * Security & Integrity: Extends the base {@link Repository} marker interface rather than
 * {@code JpaRepository} or {@code CrudRepository}. This explicitly omits all deletion and
 * batch mutation methods ({@code delete}, {@code deleteById}, {@code deleteAll}) to enforce
 * append-only semantics at the application repository contract level.
 */
@org.springframework.stereotype.Repository
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    /**
     * Persists a new audit log entry.
     *
     * @param auditLog entry to persist
     * @return persisted entry with generated ID and timestamp
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Retrieves an audit log entry by its primary key.
     *
     * @param id entry ID
     * @return optional containing entry if found
     */
    Optional<AuditLog> findById(Long id);

    /**
     * Retrieves all audit log entries.
     *
     * @return list of all entries
     */
    List<AuditLog> findAll();

    /**
     * Counts the total number of audit log entries.
     *
     * @return total count
     */
    long count();

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(Instant from, Instant to);
}
