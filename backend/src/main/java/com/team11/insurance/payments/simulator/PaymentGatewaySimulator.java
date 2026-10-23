package com.team11.insurance.payments.simulator;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Controllable simulator for external payment gateway failures (P4-05).
 * <p>
 * Supports deterministic simulation of:
 * <ul>
 *   <li>SUCCESS: standard authorization and capture.</li>
 *   <li>DECLINED: card declined / insufficient funds (business rejection, non-retryable).</li>
 *   <li>TIMEOUT: network timeout enabling verification of retry with identical idempotency key.</li>
 *   <li>GATEWAY_ERROR: 5xx internal server error.</li>
 * </ul>
 */
@Component
@Profile({"dev", "test"})
public class PaymentGatewaySimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySimulator.class);

    public enum SimulatorMode {
        SUCCESS,
        DECLINED,
        TIMEOUT,
        GATEWAY_ERROR,
        NETWORK_DISCONNECT
    }

    public record GatewayResponse(
            boolean successful,
            String transactionReference,
            String errorCode,
            String errorMessage
    ) {
        public static GatewayResponse success(String ref) {
            return new GatewayResponse(true, ref, null, null);
        }

        public static GatewayResponse declined(String code, String msg) {
            return new GatewayResponse(false, null, code, msg);
        }

        public static GatewayResponse error(String code, String msg) {
            return new GatewayResponse(false, null, code, msg);
        }
    }

    private volatile SimulatorMode defaultMode = SimulatorMode.SUCCESS;
    private final Map<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();
    private final Map<String, SimulatorMode> keyOverrides = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> timeoutAttemptsRemainingForKey = new ConcurrentHashMap<>();
    private final AtomicInteger defaultTimeoutAttemptsRemaining = new AtomicInteger(0);

    public void setMode(SimulatorMode mode) {
        this.defaultMode = mode;
        log.info("PaymentGatewaySimulator default mode set to: {}", mode);
    }

    public void setModeForKey(String idempotencyKey, SimulatorMode mode) {
        keyOverrides.put(idempotencyKey, mode);
        log.info("PaymentGatewaySimulator mode for key [{}] set to: {}", idempotencyKey, mode);
    }

    /**
     * Configure the simulator to throw {@link PaymentTimeoutException} for the next
     * {@code timeoutCount} attempts globally, after which it reverts to {@link #defaultMode}.
     */
    public void setTimeoutAttempts(int timeoutCount) {
        this.defaultTimeoutAttemptsRemaining.set(timeoutCount);
        log.info("PaymentGatewaySimulator configured for {} transient timeout attempts globally", timeoutCount);
    }

    /**
     * Configure the simulator to throw {@link PaymentTimeoutException} for the next
     * {@code timeoutCount} attempts for the specified key, after which it reverts to standard mode.
     */
    public void setTimeoutAttemptsForKey(String idempotencyKey, int timeoutCount) {
        this.timeoutAttemptsRemainingForKey.computeIfAbsent(idempotencyKey, k -> new AtomicInteger(0)).set(timeoutCount);
        log.info("PaymentGatewaySimulator configured for {} transient timeout attempts for key [{}]", timeoutCount, idempotencyKey);
    }

    public void reset() {
        this.defaultMode = SimulatorMode.SUCCESS;
        this.attemptCounters.clear();
        this.keyOverrides.clear();
        this.defaultTimeoutAttemptsRemaining.set(0);
        this.timeoutAttemptsRemainingForKey.clear();
        log.info("PaymentGatewaySimulator reset to SUCCESS with cleared counters.");
    }

    public int getAttemptsForKey(String idempotencyKey) {
        AtomicInteger counter = attemptCounters.get(idempotencyKey);
        return counter != null ? counter.get() : 0;
    }

    public GatewayResponse processPayment(String idempotencyKey, BigDecimal amount) throws PaymentTimeoutException {
        int attempt = attemptCounters.computeIfAbsent(idempotencyKey, k -> new AtomicInteger(0)).incrementAndGet();

        // Check key-specific transient timeout countdown
        AtomicInteger keyTimeouts = timeoutAttemptsRemainingForKey.get(idempotencyKey);
        if (keyTimeouts != null && keyTimeouts.getAndDecrement() > 0) {
            log.info("PaymentGatewaySimulator: simulated transient timeout for key={} (attempt {})", idempotencyKey, attempt);
            throw new PaymentTimeoutException("Gateway read timeout connecting to payment network for key: " + idempotencyKey);
        }

        // Check global transient timeout countdown
        if (defaultTimeoutAttemptsRemaining.getAndDecrement() > 0) {
            log.info("PaymentGatewaySimulator: simulated transient timeout for key={} (attempt {})", idempotencyKey, attempt);
            throw new PaymentTimeoutException("Gateway read timeout connecting to payment network for key: " + idempotencyKey);
        }

        SimulatorMode mode = keyOverrides.getOrDefault(idempotencyKey, defaultMode);

        log.info("PaymentGatewaySimulator: processing key={} amount={} attempt={} mode={}",
                idempotencyKey, amount, attempt, mode);

        return switch (mode) {
            case SUCCESS -> GatewayResponse.success("TXN-" + System.nanoTime());
            case DECLINED -> GatewayResponse.declined("PAY-402", "Card declined by issuing bank (insufficient funds)");
            case TIMEOUT -> throw new PaymentTimeoutException("Gateway read timeout connecting to payment network for key: " + idempotencyKey);
            case NETWORK_DISCONNECT -> throw new PaymentTimeoutException("Network disconnect: connection reset by peer for key: " + idempotencyKey);
            case GATEWAY_ERROR -> GatewayResponse.error("SYS-500", "Payment gateway internal server error (HTTP 502)");
        };
    }
}
