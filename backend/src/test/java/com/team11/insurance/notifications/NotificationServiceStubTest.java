package com.team11.insurance.notifications;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-06 NotificationServiceStub Unit Tests")
class NotificationServiceStubTest {

    private NotificationServiceStub notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceStub();
    }

    @Test
    @DisplayName("send() captures canonical event and parameters in thread-safe memory store")
    void testSendCanonicalEvent() {
        Map<String, String> params = Map.of("policyNo", "POL-2026-001", "amount", "15000");
        notificationService.send(NotificationEvent.POLICY_ISSUED, 101L, params);

        assertEquals(1, notificationService.count());
        List<NotificationRequest> captured = notificationService.getCapturedNotifications();
        assertEquals(1, captured.size());

        NotificationRequest req = captured.get(0);
        assertEquals(NotificationEvent.POLICY_ISSUED, req.event());
        assertEquals(101L, req.recipientUserId());
        assertEquals(NotificationChannel.EMAIL, req.channel());
        assertEquals("POL-2026-001", req.params().get("policyNo"));
        assertEquals("15000", req.params().get("amount"));
    }

    @Test
    @DisplayName("send() structured request captures custom routing details")
    void testSendStructuredRequest() {
        NotificationRequest request = new NotificationRequest(
                NotificationEvent.RENEWAL_REMINDER,
                202L,
                "customer@example.test",
                "+919876543210",
                NotificationChannel.SMS,
                Map.of("daysRemaining", "15"),
                "corr-12345"
        );

        notificationService.send(request);

        assertEquals(1, notificationService.count());
        NotificationRequest stored = notificationService.getCapturedNotifications().get(0);
        assertEquals(NotificationChannel.SMS, stored.channel());
        assertEquals("customer@example.test", stored.recipientEmail());
        assertEquals("+919876543210", stored.recipientPhone());
        assertEquals("corr-12345", stored.correlationId());
    }

    @Test
    @DisplayName("getNotificationsForUser() and getNotificationsForEvent() accurately filter recorded notifications")
    void testFilteringHelpers() {
        notificationService.send(NotificationEvent.APPLICATION_APPROVED, 101L, Map.of());
        notificationService.send(NotificationEvent.CLAIM_RECEIVED, 101L, Map.of());
        notificationService.send(NotificationEvent.APPLICATION_APPROVED, 102L, Map.of());

        assertEquals(3, notificationService.count());

        List<NotificationRequest> forUser101 = notificationService.getNotificationsForUser(101L);
        assertEquals(2, forUser101.size());

        List<NotificationRequest> forUser102 = notificationService.getNotificationsForUser(102L);
        assertEquals(1, forUser102.size());

        List<NotificationRequest> approvedEvents = notificationService.getNotificationsForEvent(NotificationEvent.APPLICATION_APPROVED);
        assertEquals(2, approvedEvents.size());

        List<NotificationRequest> claimEvents = notificationService.getNotificationsForEvent(NotificationEvent.CLAIM_RECEIVED);
        assertEquals(1, claimEvents.size());
    }

    @Test
    @DisplayName("Handles all 16 canonical notification events from SAD Section 4.1.7")
    void testAllSixteenCanonicalEvents() {
        for (NotificationEvent event : NotificationEvent.values()) {
            notificationService.send(event, 500L, Map.of("eventType", event.name()));
        }

        assertEquals(16, notificationService.count());
        for (NotificationEvent event : NotificationEvent.values()) {
            assertEquals(1, notificationService.getNotificationsForEvent(event).size(),
                    "Missing captured event for: " + event);
        }
    }

    @Test
    @DisplayName("Concurrent dispatches are thread-safe without race conditions")
    void testConcurrentSendingThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int messagesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long userId = 1000 + i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        notificationService.send(NotificationEvent.CLAIM_DOCUMENTS_REQUESTED, userId, Map.of("seq", String.valueOf(j)));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * messagesPerThread, notificationService.count());
    }

    @Test
    @DisplayName("clear() purges captured list")
    void testClear() {
        notificationService.send(NotificationEvent.PAYMENT_FAILED, 101L, Map.of());
        assertEquals(1, notificationService.count());

        notificationService.clear();
        assertEquals(0, notificationService.count());
    }
}
