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

/**
 * In-memory development and test stub for {@link IPaymentService} (Work Item: P4-06).
 * <p>
 * Simulates successful payment operations (charge, refund, payout) and strict idempotency handling
 * without interacting with third-party payment gateways.
 * Uses atomic {@link ConcurrentHashMap#computeIfAbsent(Object, java.util.function.Function)}
 * to guarantee that concurrent requests with the same idempotency key create and return the exact
 * same payment result without race conditions.
 * <p>
 * Strictly active under {@code dev} and {@code test} profiles.
 */
@Service
@Profile({"dev", "test"})
public class PaymentServiceStub implements IPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceStub.class);

    private final Clock clock;
    private final Map<String, PaymentResult> recordedPayments = new ConcurrentHashMap<>();
    private final AtomicLong paymentIdSeq = new AtomicLong(1000);

    @Autowired
    public PaymentServiceStub(ObjectProvider<Clock> clockProvider) {
        this(clockProvider.getIfAvailable(Clock::systemDefaultZone));
    }

    public PaymentServiceStub(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Override
    public PaymentResult charge(long applicationId, BigDecimal amount, String idempotencyKey) {
        log.info("PaymentServiceStub: Processing charge for applicationId={}, amount={}, key={}",
                applicationId, amount, idempotencyKey);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return recordedPayments.computeIfAbsent(idempotencyKey, k -> {
                Long paymentId = paymentIdSeq.incrementAndGet();
                String ref = "TXN-APP-" + applicationId + "-" + k;
                return PaymentResult.success(paymentId, k, ref, amount, PaymentType.CHARGE, clock);
            });
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "TXN-APP-" + applicationId + "-" + paymentId;
        return PaymentResult.success(paymentId, null, ref, amount, PaymentType.CHARGE, clock);
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
