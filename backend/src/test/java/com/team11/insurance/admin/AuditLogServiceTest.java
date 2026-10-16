package com.team11.insurance.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Persistence and service test for {@link IAuditLog} and {@link AuditLog} (P1-05).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditLogService.class)
class AuditLogServiceTest {

    @Autowired
    private IAuditLog auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void canRecordAuditLogEntryWithoutDetails() {
        auditLogService.record(101L, "LOGIN_SUCCESS", "User", 101L);

        List<AuditLog> records = auditLogRepository.findByUserId(101L);
        assertFalse(records.isEmpty());
        AuditLog entry = records.get(0);

        assertEquals(101L, entry.getUserId());
        assertEquals("LOGIN_SUCCESS", entry.getAction());
        assertEquals("User", entry.getEntity());
        assertEquals(101L, entry.getEntityId());
        assertNull(entry.getDetails());
        assertNotNull(entry.getTimestamp());
        assertTrue(entry.getTimestamp().isBefore(Instant.now().plusSeconds(5)));
    }

    @Test
    void canRecordAuditLogEntryWithDetails() {
        String detailsJson = "{\"oldStatus\":\"SUBMITTED\",\"newStatus\":\"APPROVED\",\"reason\":\"Auto-approved\"}";
        auditLogService.record(500L, "APPLICATION_APPROVED", "Application", 7001L, detailsJson);

        List<AuditLog> records = auditLogRepository.findByEntityAndEntityId("Application", 7001L);
        assertEquals(1, records.size());

        AuditLog entry = records.get(0);
        assertEquals(500L, entry.getUserId());
        assertEquals("APPLICATION_APPROVED", entry.getAction());
        assertEquals(detailsJson, entry.getDetails());
    }

    @Test
    void findByActionAndTimestampBetween() {
        Instant before = Instant.now().minus(1, ChronoUnit.MINUTES);

        auditLogService.record(201L, "POLICY_ISSUED", "Policy", 3001L);
        auditLogService.record(202L, "CLAIM_FILED", "Claim", 4001L);

        Instant after = Instant.now().plus(1, ChronoUnit.MINUTES);

        List<AuditLog> policyEvents = auditLogRepository.findByAction("POLICY_ISSUED");
        assertEquals(1, policyEvents.size());
        assertEquals(3001L, policyEvents.get(0).getEntityId());

        List<AuditLog> timeWindowLogs = auditLogRepository.findByTimestampBetween(before, after);
        assertTrue(timeWindowLogs.size() >= 2);
    }
}
