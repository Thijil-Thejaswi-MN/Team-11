package com.team11.insurance.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.payments.simulator.PaymentGatewaySimulator;
import com.team11.insurance.payments.simulator.PaymentTimeoutException;

/**
 * In-memory development and test stub for {@link IPaymentService} (Work Item: P4-06, P4-17).
 * <p>
 * Simulates payment operations (charge, refund, payout) and strict idempotency handling.
 * Integrates with {@link PaymentGatewaySimulator} when present to test failures, timeouts, and declines.
 * Uses atomic {@link ConcurrentHashMap#computeIfAbsent}
 * to guarantee that concurrent requests with the same idempotency key create and return the exact
 * same payment result without race conditions.
 * <p>
 * Strictly active under {@code dev} and {@code test} profiles.
 */
@Service
@Profile({"dev", "test"})
public class PaymentServiceStub implements IPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceStub.class);

    private static final long[] BACKOFF_DELAYS_MS = {1000L, 2000L, 4000L};
    private static final int MAX_RETRIES = 3;

    private final Clock clock;
    private final PaymentGatewaySimulator simulator;
    private PaymentRetryDelayer retryDelayer;
    private final Map<String, PaymentResult> recordedPayments = new ConcurrentHashMap<>();
    private final AtomicLong paymentIdSeq = new AtomicLong(1000);

    @Autowired
    public PaymentServiceStub(ObjectProvider<Clock> clockProvider,
                              ObjectProvider<PaymentGatewaySimulator> simulatorProvider,
                              ObjectProvider<PaymentRetryDelayer> delayerProvider) {
        this(clockProvider.getIfAvailable(Clock::systemDefaultZone),
             simulatorProvider.getIfAvailable(),
             delayerProvider.getIfAvailable(() -> PaymentRetryDelayer.REAL));
    }

    public PaymentServiceStub(Clock clock) {
        this(clock, null, PaymentRetryDelayer.REAL);
    }

    public PaymentServiceStub(Clock clock, PaymentGatewaySimulator simulator) {
        this(clock, simulator, PaymentRetryDelayer.REAL);
    }

    public PaymentServiceStub(Clock clock, PaymentGatewaySimulator simulator, PaymentRetryDelayer retryDelayer) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.simulator = simulator;
        this.retryDelayer = retryDelayer != null ? retryDelayer : PaymentRetryDelayer.REAL;
    }

    public void setRetryDelayer(PaymentRetryDelayer retryDelayer) {
        this.retryDelayer = retryDelayer != null ? retryDelayer : PaymentRetryDelayer.REAL;
    }

    public PaymentRetryDelayer getRetryDelayer() {
        return this.retryDelayer;
    }

    @Override
    public PaymentResult charge(long applicationId, BigDecimal amount, String idempotencyKey) {
        log.info("PaymentServiceStub: Processing charge for applicationId={}, amount={}, key={}",
                applicationId, amount, idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return recordedPayments.computeIfAbsent(idempotencyKey, k -> executeCharge(applicationId, amount, k));
        }

        return executeCharge(applicationId, amount, null);
    }

    private PaymentResult executeCharge(long applicationId, BigDecimal amount, String key) {
        Long paymentId = paymentIdSeq.incrementAndGet();

        if (simulator == null) {
            String ref = key != null ? "TXN-APP-" + applicationId + "-" + key : "TXN-APP-" + applicationId + "-" + paymentId;
            return PaymentResult.success(paymentId, key, ref, amount, PaymentType.CHARGE, clock);
        }

        String effectiveKey = key != null ? key : "app-" + applicationId;
        int maxAttempts = 1 + MAX_RETRIES; // 1 initial + up to 3 retries = 4 total attempts
        String lastErrorMessage = null;
        String lastErrorCode = "PAY-402";

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                long delayMs = BACKOFF_DELAYS_MS[attempt - 1];
                log.info("PaymentServiceStub: Retrying payment for key={} (attempt {}/{}) after {}ms backoff delay",
                        effectiveKey, attempt + 1, maxAttempts, delayMs);
                retryDelayer.delay(delayMs);
            }

            try {
                // Every retry within the same payment operation must reuse the exact same key and amount
                PaymentGatewaySimulator.GatewayResponse resp = simulator.processPayment(effectiveKey, amount);

                if (!resp.successful()) {
                    // Business rejection (e.g. card declined, insufficient funds): return immediately without retry
                    log.info("PaymentServiceStub: Business rejection for key={}, errorCode={}, msg={}. Not retrying.",
                            effectiveKey, resp.errorCode(), resp.errorMessage());
                    return PaymentResult.failed(paymentId, key, amount, PaymentType.CHARGE,
                            resp.errorMessage(), resp.errorCode(), clock);
                }

                // Payment succeeded
                String ref = resp.transactionReference() != null
                        ? resp.transactionReference()
                        : "TXN-APP-" + applicationId + "-" + paymentId;
                log.info("PaymentServiceStub: Payment successful on attempt {} for key={}, ref={}",
                        attempt + 1, effectiveKey, ref);
                return PaymentResult.success(paymentId, key, ref, amount, PaymentType.CHARGE, clock);

            } catch (PaymentTimeoutException pte) {
                lastErrorMessage = pte.getMessage();
                log.warn("PaymentServiceStub: Gateway timeout on attempt {}/{} for key={}: {}",
                        attempt + 1, maxAttempts, effectiveKey, pte.getMessage());
            } catch (Exception e) {
                if (isTimeoutOrNetworkException(e)) {
                    lastErrorMessage = e.getMessage() != null ? e.getMessage() : "Network timeout during payment";
                    log.warn("PaymentServiceStub: Network timeout on attempt {}/{} for key={}: {}",
                            attempt + 1, maxAttempts, effectiveKey, lastErrorMessage);
                } else {
                    log.error("PaymentServiceStub: Non-retryable error during payment for key={}", effectiveKey, e);
                    return PaymentResult.failed(paymentId, key, amount, PaymentType.CHARGE,
                            e.getMessage(), "SYS-500", clock);
                }
            }
        }

        // All attempts timed out: return contract-compatible retryable failure
        log.warn("PaymentServiceStub: All {} attempts timed out for key={}. Returning retryable failure PAY-402.",
                maxAttempts, effectiveKey);
        return PaymentResult.failed(paymentId, key, amount, PaymentType.CHARGE,
                lastErrorMessage != null ? lastErrorMessage : "Gateway timeout after maximum retries",
                lastErrorCode,
                clock);
    }

    private boolean isTimeoutOrNetworkException(Exception e) {
        if (e instanceof PaymentTimeoutException) {
            return true;
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            return lower.contains("timeout") || lower.contains("timed out") || lower.contains("disconnect") || lower.contains("connection reset");
        }
        return false;
    }

    @Override
    public PaymentResult refund(long policyId, BigDecimal amount, String idempotencyKey, String reason) {
        log.info("PaymentServiceStub: Processing refund for policyId={}, amount={}, key={}, reason={}",
                policyId, amount, idempotencyKey, reason);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return recordedPayments.computeIfAbsent(idempotencyKey, k -> {
                Long paymentId = paymentIdSeq.incrementAndGet();
                String ref = "REF-POL-" + policyId + "-" + k;
                return PaymentResult.success(paymentId, k, ref, amount, PaymentType.REFUND, clock);
            });
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "REF-POL-" + policyId + "-" + paymentId;
        return PaymentResult.success(paymentId, null, ref, amount, PaymentType.REFUND, clock);
    }

    @Override
    public PaymentResult payout(long claimId, BigDecimal assessedAmount, String idempotencyKey, String destinationAccount) {
        log.info("PaymentServiceStub: Processing payout for claimId={}, assessedAmount={}, key={}, account={}",
                claimId, assessedAmount, idempotencyKey, destinationAccount);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return recordedPayments.computeIfAbsent(idempotencyKey, k -> {
                Long paymentId = paymentIdSeq.incrementAndGet();
                String ref = "PAY-CLM-" + claimId + "-" + k;
                return PaymentResult.success(paymentId, k, ref, assessedAmount, PaymentType.PAYOUT, clock);
            });
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "PAY-CLM-" + claimId + "-" + paymentId;
        return PaymentResult.success(paymentId, null, ref, assessedAmount, PaymentType.PAYOUT, clock);
    }

    @Override
    public PaymentResult getStatus(String idempotencyKey) {
        log.info("PaymentServiceStub: Fetching status for key={}", idempotencyKey);
        if (idempotencyKey == null) {
            throw new ResourceNotFoundException("Payment attempt", "null");
        }
        PaymentResult result = recordedPayments.get(idempotencyKey);
        if (result == null) {
            throw new ResourceNotFoundException("Payment attempt", idempotencyKey);
        }
        return result;
    }

    // =========================================================================
    // Test & Diagnostic Helpers
    // =========================================================================

    public void setSyntheticFailure(String idempotencyKey, BigDecimal amount, PaymentType type, String reason, String errorCode) {
        Long paymentId = paymentIdSeq.incrementAndGet();
        PaymentResult failed = PaymentResult.failed(paymentId, idempotencyKey, amount, type, reason, errorCode, clock);
        recordedPayments.put(idempotencyKey, failed);
    }

    public void reset() {
        recordedPayments.clear();
        log.info("PaymentServiceStub: Reset recorded payments.");
    }

    public int count() {
        return recordedPayments.size();
    }
}
