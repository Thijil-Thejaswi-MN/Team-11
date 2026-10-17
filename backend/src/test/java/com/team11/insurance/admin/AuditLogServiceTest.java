package com.team11.insurance.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence, contract, and transaction semantics tests for {@link IAuditLog},
 * {@link AuditLogRepository}, and {@link AuditLog} (P1-05).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditLogService.class, AuditLogServiceTest.CallerTransactionalService.class})
class AuditLogServiceTest {

    @Autowired
    private IAuditLog auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CallerTransactionalService callerService;

    /**
     * Helper service to simulate a business transaction that invokes the audit log
     * and subsequently fails, testing {@link Propagation#REQUIRES_NEW} survival.
     */
    static class CallerTransactionalService {
        private final IAuditLog auditLog;

        public CallerTransactionalService(IAuditLog auditLog) {
            this.auditLog = auditLog;
        }

        @Transactional
        public void executeAndRollback(long userId, String action, String entity, long entityId, String details) {
            auditLog.record(userId, action, entity, entityId, details);
            throw new RuntimeException("Simulated caller business transaction failure");
        }
    }

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

    @Test
    void recordsSystemGeneratedActionsWithSentinelUserId() {
        // Test convenience overload without details
        auditLogService.recordSystem("BATCH_EXPIRE_QUOTES", "Quote", 0L);

        // Test convenience overload with details
        String autoUwDetails = "{\"decision\":\"APPROVE\",\"reason\":\"STUB_RULE_MATCH\"}";
        auditLogService.recordSystem("AUTO_UNDERWRITE", "Application", 8801L, autoUwDetails);

        List<AuditLog> systemLogs = auditLogRepository.findByUserId(IAuditLog.SYSTEM_USER_ID);
        assertEquals(2, systemLogs.size());

        AuditLog first = systemLogs.stream()
                .filter(log -> "BATCH_EXPIRE_QUOTES".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertEquals(IAuditLog.SYSTEM_USER_ID, first.getUserId());
        assertEquals(0L, first.getEntityId());
        assertNull(first.getDetails());

        AuditLog second = systemLogs.stream()
                .filter(log -> "AUTO_UNDERWRITE".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertEquals(IAuditLog.SYSTEM_USER_ID, second.getUserId());
        assertEquals("Application", second.getEntity());
        assertEquals(8801L, second.getEntityId());
        assertEquals(autoUwDetails, second.getDetails());
    }

    @Test
    void enforceAppendOnlyRepositoryContract() {
        // Verify repository extends base marker Repository, NOT CrudRepository or JpaRepository
        assertTrue(Repository.class.isAssignableFrom(AuditLogRepository.class));
        assertFalse(CrudRepository.class.isAssignableFrom(AuditLogRepository.class),
                "AuditLogRepository must not extend CrudRepository");

        // Verify no delete methods are declared or inherited by the repository contract
        Method[] methods = AuditLogRepository.class.getMethods();
        List<String> deleteMethods = Arrays.stream(methods)
                .map(Method::getName)
                .filter(name -> name.startsWith("delete"))
                .toList();
        assertTrue(deleteMethods.isEmpty(),
                "AuditLogRepository must not expose any delete methods, found: " + deleteMethods);

        // Verify AuditLog entity has no setter methods to maintain Java-level immutability
        Method[] entityMethods = AuditLog.class.getDeclaredMethods();
        List<String> setterMethods = Arrays.stream(entityMethods)
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();
        assertTrue(setterMethods.isEmpty(),
                "AuditLog entity must have no setter methods, found: " + setterMethods);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditLogPersistsEvenWhenCallerTransactionRollsBack() {
        long isolatedUserId = 9999L;
        String action = "ROLLBACK_TEST_ACTION";

        // Invoke caller transactional method that writes audit then throws RuntimeException
        assertThrows(RuntimeException.class, () ->
                callerService.executeAndRollback(isolatedUserId, action, "Payment", 777L, "payload"));

        // Verify the audit log was committed independently via REQUIRES_NEW despite caller rollback
        List<AuditLog> records = auditLogRepository.findByUserId(isolatedUserId);
        assertEquals(1, records.size(),
                "AuditLog must survive caller transaction rollback due to Propagation.REQUIRES_NEW");
        AuditLog entry = records.get(0);
        assertEquals(action, entry.getAction());
        assertEquals("Payment", entry.getEntity());
        assertEquals(777L, entry.getEntityId());
    }

    @Test
    void rejectsNullUserOrEntityParameters() {
        // SAD §3.9 and IAuditLog contract require non-null userId for non-repudiation.
        assertThrows(NullPointerException.class, () ->
                new AuditLog(null, "LOGIN_FAILED", "User", 101L, null));
        assertThrows(NullPointerException.class, () ->
                new AuditLog(101L, null, "User", 101L, null));
        assertThrows(NullPointerException.class, () ->
                new AuditLog(101L, "LOGIN_SUCCESS", null, 101L, null));
        assertThrows(NullPointerException.class, () ->
                new AuditLog(101L, "LOGIN_SUCCESS", "User", null, null));
        assertThrows(NullPointerException.class, () ->
                auditLogService.record(101L, null, "User", 101L));
        assertThrows(NullPointerException.class, () ->
                auditLogService.record(101L, "LOGIN_SUCCESS", null, 101L));
    }
}
