package com.team11.insurance.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.team11.insurance.common.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-06 PaymentServiceStub Unit Tests")
class PaymentServiceStubTest {

    private final Instant fixedTime = Instant.parse("2026-10-21T10:00:00Z");
    private final Clock clock = Clock.fixed(fixedTime, ZoneId.of("UTC"));
    private PaymentServiceStub paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceStub(clock);
    }

    @Test
    @DisplayName("charge() returns simulated SUCCESS with deterministic reference and injected clock time")
    void testChargeSuccess() {
        BigDecimal amount = new BigDecimal("12500.00");
        String key = "app-1001-attempt-1";

        PaymentResult result = paymentService.charge(1001L, amount, key);

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.status());
        assertEquals(PaymentType.CHARGE, result.type());
        assertEquals(key, result.idempotencyKey());
        assertEquals(amount, result.amount());
        assertEquals(fixedTime, result.processedAt());
        assertNotNull(result.paymentId());
        assertTrue(result.transactionReference().startsWith("TXN-APP-1001-"));
        assertNull(result.failureReason());
        assertNull(result.errorCode());
    }

    @Test
    @DisplayName("charge() is idempotent: repeated call with same key returns identical cached result")
    void testChargeIdempotency() {
        BigDecimal amount = new BigDecimal("8500.00");
        String key = "app-2002-attempt-1";

        PaymentResult first = paymentService.charge(2002L, amount, key);
        PaymentResult second = paymentService.charge(2002L, amount, key);

        assertSame(first, second, "Should return cached reference for identical idempotency key");
        assertEquals(first.paymentId(), second.paymentId());
        assertEquals(first.transactionReference(), second.transactionReference());
        assertEquals(1, paymentService.count());
    }

    @Test
    @DisplayName("Concurrent charge() calls with identical idempotencyKey atomically produce identical result exactly once")
    void testConcurrentChargeIdempotency() throws Exception {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        String key = "concurrent-key-999";
        BigDecimal amount = new BigDecimal("15000.00");
        List<PaymentResult> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    PaymentResult res = paymentService.charge(9999L, amount, key);
                    results.add(res);
                } catch (Exception e) {
                    fail("Concurrent execution threw unexpected exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threads, results.size());
        PaymentResult baseline = results.get(0);
        assertNotNull(baseline);
        for (PaymentResult r : results) {
            assertSame(baseline, r, "Every concurrent thread must receive the exact same cached instance");
            assertEquals(baseline.paymentId(), r.paymentId());
            assertEquals(baseline.transactionReference(), r.transactionReference());
        }
        assertEquals(1, paymentService.count(), "Only 1 payment record must be created for the key");
    }

    @Test
    @DisplayName("refund() returns simulated SUCCESS and honors idempotency")
    void testRefundSuccessAndIdempotency() {
        BigDecimal refundAmount = new BigDecimal("4500.00");
        String key = "cancel-policy-3003-req-1";

        PaymentResult first = paymentService.refund(3003L, refundAmount, key, "Customer cancellation");
        assertNotNull(first);
        assertEquals(PaymentStatus.SUCCESS, first.status());
        assertEquals(PaymentType.REFUND, first.type());
        assertEquals(refundAmount, first.amount());
        assertTrue(first.transactionReference().startsWith("REF-POL-3003-"));

        PaymentResult second = paymentService.refund(3003L, refundAmount, key, "Customer cancellation");
        assertSame(first, second);
    }

    @Test
    @DisplayName("payout() returns simulated SUCCESS and honors idempotency")
    void testPayoutSuccessAndIdempotency() {
        BigDecimal payoutAmount = new BigDecimal("25000.00");
        String key = "claim-4004-payout-1";

        PaymentResult first = paymentService.payout(4004L, payoutAmount, key, "UPI:asha@bank");
        assertNotNull(first);
        assertEquals(PaymentStatus.SUCCESS, first.status());
        assertEquals(PaymentType.PAYOUT, first.type());
        assertEquals(payoutAmount, first.amount());
        assertTrue(first.transactionReference().startsWith("PAY-CLM-4004-"));

        PaymentResult second = paymentService.payout(4004L, payoutAmount, key, "UPI:asha@bank");
        assertSame(first, second);
    }

    @Test
    @DisplayName("getStatus() retrieves existing payment by key or throws ResourceNotFoundException")
    void testGetStatus() {
        String key = "status-test-key";
        paymentService.charge(5005L, new BigDecimal("1000.00"), key);

        PaymentResult found = paymentService.getStatus(key);
        assertNotNull(found);
        assertEquals(key, found.idempotencyKey());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getStatus("unknown-key"));
        assertThrows(ResourceNotFoundException.class, () -> paymentService.getStatus(null));
    }

    @Test
    @DisplayName("Synthetic failure override and reset helper function as expected")
    void testSyntheticFailureAndReset() {
        String key = "synthetic-fail-key";
        paymentService.setSyntheticFailure(key, new BigDecimal("5000.00"), PaymentType.CHARGE, "Card expired", "PAY-402");

        PaymentResult result = paymentService.getStatus(key);
        assertEquals(PaymentStatus.FAILED, result.status());
        assertEquals("Card expired", result.failureReason());
        assertEquals("PAY-402", result.errorCode());

        paymentService.reset();
        assertEquals(0, paymentService.count());
    }
}
