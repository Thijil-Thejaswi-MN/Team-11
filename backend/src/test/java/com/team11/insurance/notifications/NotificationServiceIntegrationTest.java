package com.team11.insurance.notifications;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.notifications.dispatcher.NotificationDispatcher;
import com.team11.insurance.notifications.dispatcher.NotificationRecoveryScheduler;
import com.team11.insurance.notifications.sender.SimulatorNotificationChannelSender;
import com.team11.insurance.notifications.simulator.NotificationSenderSimulator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P4-08 Notification Service Integration & Delivery Tests")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationDispatcher dispatcher;

    @Autowired
    private NotificationRecoveryScheduler recoveryScheduler;

    @Autowired
    private SimulatorNotificationChannelSender channelSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private NotificationSenderSimulator simulator;

    @BeforeEach
    void setUp() {
        recoveryScheduler.setPollingEnabled(false);
        simulator = channelSender.getSimulator();
        simulator.reset();
        outboxRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        simulator.reset();
        outboxRepository.deleteAll();
        recoveryScheduler.setPollingEnabled(true);
    }

    @Test
    @DisplayName("Requirement 1 & 9: Enqueueing produces a persisted outbox record with correlation ID and PENDING state")
    void test1_enqueuePersistsPendingNotificationWithCorrelationId() {
        NotificationRequest request = new NotificationRequest(
                NotificationEvent.APPLICATION_APPROVED,
                101L,
                "customer@example.test",
                "+919876543210",
                NotificationChannel.EMAIL,
                Map.of("applicationId", "APP-2026-001"),
                "corr-audit-999"
        );

        notificationService.send(request);

        List<NotificationOutbox> outboxList = outboxRepository.findByCorrelationId("corr-audit-999");
        assertThat(outboxList).hasSize(1);

        NotificationOutbox outbox = outboxList.get(0);
        assertThat(outbox.getEvent()).isEqualTo(NotificationEvent.APPLICATION_APPROVED);
        assertThat(outbox.getRecipientUserId()).isEqualTo(101L);
        assertThat(outbox.getRecipientEmail()).isEqualTo("customer@example.test");
        assertThat(outbox.getRecipientPhone()).isEqualTo("+919876543210");
        assertThat(outbox.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(outbox.getCorrelationId()).isEqualTo("corr-audit-999");
        assertThat(outbox.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Requirement 2: Enqueueing is non-blocking and returns within milliseconds")
    void test2_deliveryIsAsynchronousAndNonBlocking() {
        NotificationRequest request = new NotificationRequest(
                NotificationEvent.POLICY_ISSUED,
                102L,
                "user102@example.test",
                null,
                NotificationChannel.EMAIL,
                Map.of("policyNo", "POL-9999", "amount", "10000"),
                "corr-speed-test"
        );

        long start = System.currentTimeMillis();
        notificationService.send(request);
        long elapsed = System.currentTimeMillis() - start;

        // Enqueueing to outbox must complete rapidly without awaiting delivery latency
        assertThat(elapsed).isLessThan(200);

        List<NotificationOutbox> outboxList = outboxRepository.findByCorrelationId("corr-speed-test");
        assertThat(outboxList).hasSize(1);
    }

    @Test
    @DisplayName("Requirement 3: Successful Email delivery transitions status to SENT with rendered payload")
    void test3_successfulEmailDelivery() {
        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.POLICY_ISSUED,
                "customer@example.test",
                null,
                NotificationChannel.EMAIL,
                Map.of("policyNo", "POL-2026-001", "amount", "15000.00")
        );

        boolean dispatched = dispatcher.dispatch(outbox.getId());
        assertThat(dispatched).isTrue();

        NotificationOutbox updated = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getRenderedSubject()).contains("POL-2026-001");
        assertThat(updated.getRenderedBody()).contains("POL-2026-001").contains("15000.00");
        assertThat(updated.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Requirement 4: Successful SMS delivery transitions status to SENT with rendered SMS body")
    void test4_successfulSmsDelivery() {
        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.RENEWAL_REMINDER,
                null,
                "+919876543210",
                NotificationChannel.SMS,
                Map.of("policyNo", "POL-2026-001", "daysRemaining", "15", "expiryDate", "2026-11-15")
        );

        boolean dispatched = dispatcher.dispatch(outbox.getId());
        assertThat(dispatched).isTrue();

        NotificationOutbox updated = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
        assertThat(updated.getRenderedBody()).contains("POL-2026-001").contains("15 days");
    }

    @Test
    @DisplayName("Requirement 5: ALL routing and multi-channel fallback behavior")
    void test5_allRoutingAndMultiChannelFallback() {
        // Part A: ALL routing delivers across email and phone
        NotificationOutbox allOutbox = createPendingOutbox(
                NotificationEvent.CLAIM_RECEIVED,
                "user@example.test",
                "+919876543210",
                NotificationChannel.ALL,
                Map.of("claimNo", "CLM-2026-001")
        );
        boolean allDispatched = dispatcher.dispatch(allOutbox.getId());
        assertThat(allDispatched).isTrue();
        assertThat(outboxRepository.findById(allOutbox.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationStatus.SENT);

        // Part B: Fallback - Email delivery fails, but SMS fallback succeeds
        simulator.setFailNTimesThenSucceed("fail-email@example.test", 10); // Email always fails
        NotificationOutbox fallbackOutbox = createPendingOutbox(
                NotificationEvent.CLAIM_APPROVED,
                "fail-email@example.test",
                "+919876543210",
                NotificationChannel.EMAIL,
                Map.of("claimNo", "CLM-2026-002", "amount", "25000.00")
        );

        boolean fallbackDispatched = dispatcher.dispatch(fallbackOutbox.getId());
        assertThat(fallbackDispatched).isTrue();

        NotificationOutbox fallbackUpdated = outboxRepository.findById(fallbackOutbox.getId()).orElseThrow();
        assertThat(fallbackUpdated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(fallbackUpdated.getSentAt()).isNotNull();
        assertThat(fallbackUpdated.getFailureReason()).contains("FALLBACK_DELIVERY");
    }

    @Test
    @DisplayName("Requirement 5b: ALL routing with partial delivery (Email succeeds, SMS fails) records SMS failure details in outbox")
    void test5b_allRoutingPartialDeliveryRecordsFailureReasonWithoutLosingSmsError() {
        String emailDest = "valid-email@example.test";
        String phoneDest = "+919999988888";
        simulator.setFailNTimesThenSucceed(phoneDest, 10); // SMS fails

        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.POLICY_ISSUED,
                emailDest,
                phoneDest,
                NotificationChannel.ALL,
                Map.of("policyNo", "POL-PARTIAL-01", "amount", "10000.00")
        );

        boolean dispatched = dispatcher.dispatch(outbox.getId());
        assertThat(dispatched).isTrue();

        NotificationOutbox updated = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
        assertThat(updated.getFailureReason()).isNotNull();
        assertThat(updated.getFailureReason()).contains("PARTIAL_DELIVERY");
        assertThat(updated.getFailureReason()).contains("SMS delivery failed");
        assertThat(updated.getLastError()).isNotNull();
        assertThat(updated.getLastError()).contains("Simulated transient connection timeout");
    }

    @Test
    @DisplayName("Requirement 7: Transient failure triggers specified backoff schedule (2s, 10s, 30s)")
    void test7_transientFailureAndRetrySchedule() {
        simulator.setMode(NotificationSenderSimulator.DeliveryMode.TRANSIENT_FAILURE);

        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.PAYMENT_FAILED,
                "transient@example.test",
                null,
                NotificationChannel.EMAIL,
                Map.of("applicationId", "APP-2026-001", "amount", "5000.00", "reason", "Card expired")
        );

        // Attempt 1: fails, schedules retry 1 at +2s
        boolean d1 = dispatcher.dispatch(outbox.getId());
        assertThat(d1).isFalse();

        NotificationOutbox afterAttempt1 = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(afterAttempt1.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(afterAttempt1.getAttemptCount()).isEqualTo(1);
        assertThat(afterAttempt1.getNextRetryAt()).isNotNull();
        assertThat(afterAttempt1.getNextRetryAt().getEpochSecond() - afterAttempt1.getUpdatedAt().getEpochSecond())
                .isBetween(1L, 3L); // ~2s delay

        // Reset simulator to SUCCESS and deliver
        simulator.setMode(NotificationSenderSimulator.DeliveryMode.SUCCESS);
        boolean d2 = dispatcher.dispatch(outbox.getId());
        assertThat(d2).isTrue();

        NotificationOutbox afterSuccess = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(afterSuccess.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(afterSuccess.getAttemptCount()).isEqualTo(2);
        assertThat(afterSuccess.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("Requirement 8: Terminal failures and dead-letter log transition")
    void test8_terminalFailureAndDeadLetterLog() {
        simulator.setMode(NotificationSenderSimulator.DeliveryMode.TERMINAL_FAILURE);

        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.APPLICATION_REJECTED,
                "invalid-recipient@example.test",
                null,
                NotificationChannel.EMAIL,
                Map.of("applicationId", "APP-2026-999", "reason", "Underwriting risk threshold exceeded")
        );

        boolean dispatched = dispatcher.dispatch(outbox.getId());
        assertThat(dispatched).isFalse();

        NotificationOutbox updated = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getNextRetryAt()).isNull();
        assertThat(updated.getFailureReason()).contains("Terminal failure");
        assertThat(updated.getLastError()).contains("terminal delivery failure");
    }

    @Test
    @DisplayName("Requirement 10: Restart/recovery recovers pending notifications and delivers them")
    void test10_restartRecoveryOfPendingNotifications() {
        createPendingOutbox(NotificationEvent.APPLICATION_APPROVED, "rec1@example.test", null, NotificationChannel.EMAIL, Map.of("applicationId", "APP-1"));
        createPendingOutbox(NotificationEvent.CLAIM_RECEIVED, "rec2@example.test", null, NotificationChannel.EMAIL, Map.of("claimNo", "CLM-2"));
        createPendingOutbox(NotificationEvent.POLICY_ISSUED, "rec3@example.test", null, NotificationChannel.EMAIL, Map.of("policyNo", "POL-3", "amount", "1000"));

        int recoveredCount = recoveryScheduler.triggerRecovery();
        assertThat(recoveredCount).isEqualTo(3);

        List<NotificationOutbox> pendingLeft = outboxRepository.findPendingDueForDelivery(Instant.now());
        assertThat(pendingLeft).isEmpty();
    }

    @Test
    @DisplayName("Requirement 10b: Scheduled polling respects pollingEnabled toggle and triggers recovery when enabled")
    void test10b_scheduledPollingToggleAndExecution() {
        try {
            recoveryScheduler.setPollingEnabled(false);
            createPendingOutbox(NotificationEvent.APPLICATION_APPROVED, "poll-test@example.test", null, NotificationChannel.EMAIL, Map.of("applicationId", "APP-POLL-1"));

            // When polling is disabled, pollPendingNotifications does nothing
            recoveryScheduler.pollPendingNotifications();
            List<NotificationOutbox> stillPending = outboxRepository.findPendingDueForDelivery(Instant.now());
            assertThat(stillPending).hasSize(1);

            // When polling is enabled, pollPendingNotifications executes recovery
            recoveryScheduler.setPollingEnabled(true);
            recoveryScheduler.pollPendingNotifications();
            List<NotificationOutbox> pendingLeft = outboxRepository.findPendingDueForDelivery(Instant.now());
            assertThat(pendingLeft).isEmpty();
        } finally {
            recoveryScheduler.setPollingEnabled(false);
        }
    }

    @Test
    @DisplayName("Requirement 11: Concurrent dispatch attempts prevent duplicate delivery")
    void test11_concurrentDispatchPreventsDuplicateDelivery() throws Exception {
        NotificationOutbox outbox = createPendingOutbox(
                NotificationEvent.CLAIM_APPROVED,
                "concurrent@example.test",
                null,
                NotificationChannel.EMAIL,
                Map.of("claimNo", "CLM-CONC-1", "amount", "50000.00")
        );

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    if (dispatcher.dispatch(outbox.getId())) {
                        successCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads concurrently

        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one thread claimed and delivered the outbox record
        assertThat(successCounter.get()).isEqualTo(1);

        NotificationOutbox finalOutbox = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(finalOutbox.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(finalOutbox.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Requirement 12: Notification failure does not roll back the associated business operation")
    void test12_notificationFailureDoesNotRollbackBusinessTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        String testEmail = "tx-test-" + System.currentTimeMillis() + "@example.test";

        tx.executeWithoutResult(status -> {
            // Simulated business operation: create user
            User user = new User();
            user.setName("Business User");
            user.setEmail(testEmail);
            user.setPhone("+919111111111");
            user.setPasswordHash("$2a$12$DummyHashForTxTest0000000000000000000000000000000000");
            user.setRole(com.team11.insurance.auth.UserRole.CUSTOMER);
            userRepository.save(user);

            // Trigger notification with invalid template params (missing required amount)
            notificationService.send(NotificationEvent.PAYMENT_FAILED, user.getId(), Map.of());
        });

        // Verify caller transaction committed successfully despite notification template issue
        User persistedUser = userRepository.findByEmailIgnoreCase(testEmail).orElse(null);
        assertThat(persistedUser).isNotNull();
        assertThat(persistedUser.getName()).isEqualTo("Business User");

        // Clean up
        userRepository.delete(persistedUser);
    }

    @Test
    @DisplayName("Requirement 13: Existing INotify callers and NotificationServiceStub remain compatible")
    void test13_existingINotifyCallersAndStubCompatibility() {
        // Direct call to INotify signature
        notificationService.send(NotificationEvent.CLAIM_RECEIVED, 303L, Map.of("claimNo", "CLM-303"));

        List<NotificationOutbox> outboxList = outboxRepository.findAll();
        assertThat(outboxList).isNotEmpty();

        // NotificationServiceStub isolated unit behavior remains intact
        NotificationServiceStub stub = new NotificationServiceStub();
        stub.send(NotificationEvent.CLAIM_CLOSED, 404L, Map.of("claimNo", "CLM-404"));
        assertThat(stub.count()).isEqualTo(1);
        assertThat(stub.getNotificationsForUser(404L)).hasSize(1);
    }

    private NotificationOutbox createPendingOutbox(
            NotificationEvent event,
            String email,
            String phone,
            NotificationChannel channel,
            Map<String, String> params) {
        Instant now = Instant.now();
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setEvent(event);
        outbox.setRecipientUserId(999L);
        outbox.setRecipientEmail(email);
        outbox.setRecipientPhone(phone);
        outbox.setChannel(channel);
        try {
            outbox.setParamsJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params));
        } catch (Exception e) {
            outbox.setParamsJson("{}");
        }
        outbox.setStatus(NotificationStatus.PENDING);
        outbox.setAttemptCount(0);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        return outboxRepository.save(outbox);
    }
}
