package com.team11.insurance.admin;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing the {@link IAuditLog} interface for immutable audit trail recording.
 */
@Service
public class AuditLogService implements IAuditLog {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(long userId, String action, String entity, long entityId) {
        record(userId, action, entity, entityId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(long userId, String action, String entity, long entityId, String details) {
        AuditLog auditLog = new AuditLog(userId, action, entity, entityId, details);
        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("AuditLog recorded: id={}, user={}, action={}, entity={}:{}",
                saved.getId(), userId, action, entity, entityId);
    }
}
