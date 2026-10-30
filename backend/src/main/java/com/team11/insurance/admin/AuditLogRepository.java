package com.team11.insurance.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    /**
     * Retrieves paginated audit logs filtered by optional criteria, sorted newest first.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:action IS NULL OR UPPER(a.action) = UPPER(:action)) AND " +
            "(:entity IS NULL OR UPPER(a.entity) = UPPER(:entity)) AND " +
            "(:entityId IS NULL OR a.entityId = :entityId) AND " +
            "(:from IS NULL OR a.timestamp >= :from) AND " +
            "(:to IS NULL OR a.timestamp <= :to) " +
            "ORDER BY a.timestamp DESC, a.id DESC")
    Page<AuditLog> findWithFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entity") String entity,
            @Param("entityId") Long entityId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
