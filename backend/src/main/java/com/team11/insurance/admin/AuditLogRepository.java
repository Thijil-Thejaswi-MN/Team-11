package com.team11.insurance.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for AuditLog entries.
 * Supports query operations for System Admin audit views and verification tests.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByEntityAndEntityId(String entity, Long entityId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(Instant from, Instant to);
}
