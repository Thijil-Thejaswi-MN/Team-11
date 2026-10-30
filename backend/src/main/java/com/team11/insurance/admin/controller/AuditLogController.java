package com.team11.insurance.admin.controller;

import com.team11.insurance.admin.AuditLog;
import com.team11.insurance.admin.AuditLogRepository;
import com.team11.insurance.admin.dto.AuditLogEntryResponse;
import com.team11.insurance.admin.dto.AuditLogPageResponse;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.ValidationException;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for immutable audit trail querying (INS-F-018, INS-SR-008, SAD §4.3.6 & §5.2).
 * <p>
 * Base path: {@code /audit} (serviced under servlet context-path {@code /api/v1}).
 * Restricted exclusively to {@link UserRole#SYSTEM_ADMIN}.
 * Exposes read-only queries with filtering, sorting (newest first), and pagination.
 * Append-only: never accepts or returns mutation (UPDATE/DELETE) operations.
 */
@RestController
@RequestMapping("/audit")
public class AuditLogController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Retrieves paginated, filtered audit log entries (SAD §5.2, INS-F-018, TC-Aud-01).
     * Accessible only by System Admin.
     *
     * @param userId optional filter by acting user ID
     * @param action optional filter by action code
     * @param entity optional filter by entity type
     * @param entityId optional filter by entity ID
     * @param from optional ISO start timestamp/date
     * @param to optional ISO end timestamp/date
     * @param page 0-based page index (default 0)
     * @param size page size (default 20, max 100)
     * @param currentUser authenticated identity context
     * @return paginated list of masked audit log entries
     */
    @GetMapping({"", "/logs"})
    public ResponseEntity<AuditLogPageResponse> getAuditLogs(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "entity", required = false) String entity,
            @RequestParam(name = "entityId", required = false) Long entityId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            CurrentUser currentUser) {

        // Enforce RBAC: SYSTEM_ADMIN only (INS-SR-003, SAD §4.3.6)
        RoleSecurity.assertRole(currentUser, UserRole.SYSTEM_ADMIN);

        // Validate pagination arguments
        if (page == null || page < 0) {
            throw new ValidationException("page", "Page index must not be negative");
        }
        if (size == null || size <= 0 || size > 100) {
            throw new ValidationException("size", "Page size must be between 1 and 100");
        }

        // Validate optional numeric filters
        if (userId != null && userId <= 0) {
            throw new ValidationException("userId", "userId must be a positive number");
        }
        if (entityId != null && entityId <= 0) {
            throw new ValidationException("entityId", "entityId must be a positive number");
        }

        // Parse and validate ISO timestamps
        Instant fromInstant = parseIsoTimestamp(from, true, "from");
        Instant toInstant = parseIsoTimestamp(to, false, "to");

        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ValidationException("from", "'from' timestamp must not be after 'to' timestamp");
        }

        log.debug("GET /audit query: userId={}, action={}, entity={}, entityId={}, from={}, to={}, page={}, size={}",
                userId, action, entity, entityId, fromInstant, toInstant, page, size);

        // Query repository with optional criteria and pagination (newest first: timestamp DESC, id DESC)
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> pagedResult = auditLogRepository.findWithFilters(userId, action, entity, entityId, fromInstant, toInstant, pageable);

        List<AuditLogEntryResponse> items = pagedResult.getContent().stream()
                .map(AuditLogEntryResponse::fromEntity)
                .toList();

        AuditLogPageResponse response = new AuditLogPageResponse(
                items,
                pagedResult.getNumber(),
                pagedResult.getSize(),
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Parses ISO-8601 strings into Instants.
     * Supports full instants (e.g. 2026-10-15T10:00:00Z, +05:30) and date-only (e.g. 2026-10-15).
     */
    private Instant parseIsoTimestamp(String dateStr, boolean isStartOfDay, String paramName) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String clean = dateStr.trim();
        try {
            return Instant.parse(clean);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(clean).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate localDate = LocalDate.parse(clean);
            if (isStartOfDay) {
                return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            } else {
                return localDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
            }
        } catch (DateTimeParseException ignored) {
        }

        throw new ValidationException(paramName, "Invalid ISO date/timestamp format for parameter '" + paramName + "': " + dateStr);
    }
}
