package com.team11.insurance.admin.dto;

import com.team11.insurance.admin.AuditLog;
import com.team11.insurance.common.logging.SensitiveDataMasker;

import java.time.Instant;

/**
 * DTO representing an individual immutable audit log entry (INS-F-018, INS-SR-008).
 * Ensures sensitive data such as PAN, Aadhaar, passwords, and tokens are scrubbed from details.
 */
public class AuditLogEntryResponse {

    private Long id;
    private Long userId;
    private String action;
    private String entity;
    private Long entityId;
    private String details;
    private Instant timestamp;

    public AuditLogEntryResponse() {
    }

    public AuditLogEntryResponse(Long id, Long userId, String action, String entity, Long entityId, String details, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.details = details != null ? SensitiveDataMasker.maskGeneric(details) : null;
        this.timestamp = timestamp;
    }

    public static AuditLogEntryResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }
        return new AuditLogEntryResponse(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntity(),
                log.getEntityId(),
                log.getDetails(),
                log.getTimestamp()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details != null ? SensitiveDataMasker.maskGeneric(details) : null;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
