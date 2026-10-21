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

        if (idempotencyKey != null && recordedPayments.containsKey(idempotencyKey)) {
            log.info("PaymentServiceStub: Returning cached result for idempotencyKey={}", idempotencyKey);
            return recordedPayments.get(idempotencyKey);
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "TXN-APP-" + applicationId + "-" + (idempotencyKey != null ? idempotencyKey : paymentId);
        PaymentResult result = PaymentResult.success(paymentId, idempotencyKey, ref, amount, PaymentType.CHARGE, clock);

        if (idempotencyKey != null) {
            recordedPayments.put(idempotencyKey, result);
        }
        return result;
    }

    @Override
    public PaymentResult refund(long policyId, BigDecimal amount, String idempotencyKey, String reason) {
        log.info("PaymentServiceStub: Processing refund for policyId={}, amount={}, key={}, reason={}",
                policyId, amount, idempotencyKey, reason);

        if (idempotencyKey != null && recordedPayments.containsKey(idempotencyKey)) {
            log.info("PaymentServiceStub: Returning cached refund result for idempotencyKey={}", idempotencyKey);
            return recordedPayments.get(idempotencyKey);
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "REF-POL-" + policyId + "-" + (idempotencyKey != null ? idempotencyKey : paymentId);
        PaymentResult result = PaymentResult.success(paymentId, idempotencyKey, ref, amount, PaymentType.REFUND, clock);

        if (idempotencyKey != null) {
            recordedPayments.put(idempotencyKey, result);
        }
        return result;
    }

    @Override
    public PaymentResult payout(long claimId, BigDecimal assessedAmount, String idempotencyKey, String destinationAccount) {
        log.info("PaymentServiceStub: Processing payout for claimId={}, assessedAmount={}, key={}, account={}",
                claimId, assessedAmount, idempotencyKey, destinationAccount);

        if (idempotencyKey != null && recordedPayments.containsKey(idempotencyKey)) {
            log.info("PaymentServiceStub: Returning cached payout result for idempotencyKey={}", idempotencyKey);
            return recordedPayments.get(idempotencyKey);
        }

        Long paymentId = paymentIdSeq.incrementAndGet();
        String ref = "PAY-CLM-" + claimId + "-" + (idempotencyKey != null ? idempotencyKey : paymentId);
        PaymentResult result = PaymentResult.success(paymentId, idempotencyKey, ref, assessedAmount, PaymentType.PAYOUT, clock);

        if (idempotencyKey != null) {
            recordedPayments.put(idempotencyKey, result);
        }
        return result;
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
