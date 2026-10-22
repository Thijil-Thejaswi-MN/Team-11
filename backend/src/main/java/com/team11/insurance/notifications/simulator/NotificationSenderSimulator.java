package com.team11.insurance.notifications.simulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Controllable simulator for messaging and notification delivery failures (P4-05).
 * <p>
 * Supports deterministic simulation of:
 * <ul>
 *   <li>SUCCESS: instant delivery across SMS and Email channels.</li>
 *   <li>TRANSIENT_FAILURE: SMTP / SMS gateway temporary outage (retryable).</li>
 *   <li>TERMINAL_FAILURE: invalid recipient / mailbox full (non-retryable, moves to dead-letter log).</li>
 * </ul>
 */
@Component
@Profile({"dev", "test"})
public class NotificationSenderSimulator {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderSimulator.class);

    public enum DeliveryMode {
        SUCCESS,
        TRANSIENT_FAILURE,
        TERMINAL_FAILURE
    }

    private volatile DeliveryMode defaultMode = DeliveryMode.SUCCESS;
    private final Map<String, AtomicInteger> deliveryAttempts = new ConcurrentHashMap<>();
    private final Map<String, Integer> failNTimesThenSucceed = new ConcurrentHashMap<>();

    public void setMode(DeliveryMode mode) {
        this.defaultMode = mode;
        log.info("NotificationSenderSimulator default mode set to: {}", mode);
    }

    public void setFailNTimesThenSucceed(String identifier, int failures) {
        this.failNTimesThenSucceed.put(identifier, failures);
        log.info("NotificationSenderSimulator: identifier [{}] will fail {} times before succeeding", identifier, failures);
    }

    public int getAttempts(String identifier) {
        AtomicInteger counter = deliveryAttempts.get(identifier);
        return counter != null ? counter.get() : 0;
    }

    public void reset() {
        this.defaultMode = DeliveryMode.SUCCESS;
        this.deliveryAttempts.clear();
        this.failNTimesThenSucceed.clear();
        log.info("NotificationSenderSimulator reset to SUCCESS with cleared counters.");
    }

    public boolean simulateDelivery(String identifier, String channel, String destination) throws NotificationDeliveryException {
        int attempt = deliveryAttempts.computeIfAbsent(identifier, k -> new AtomicInteger(0)).incrementAndGet();

        Integer failLimit = failNTimesThenSucceed.get(identifier);
        if (failLimit != null) {
            if (attempt <= failLimit) {
                log.warn("NotificationSenderSimulator: simulating transient failure {} of {} for {}", attempt, failLimit, identifier);
                throw new NotificationDeliveryException("Simulated transient connection timeout to SMS/SMTP provider (attempt " + attempt + ")");
            } else {
                log.info("NotificationSenderSimulator: transient failure threshold reached, delivery SUCCEEDED on attempt {} for {}", attempt, identifier);
                return true;
            }
        }

        return switch (defaultMode) {
            case SUCCESS -> {
                log.info("NotificationSenderSimulator: delivery SUCCEEDED to channel [{}] destination [{}] on attempt {}", channel, destination, attempt);
                yield true;
            }
            case TRANSIENT_FAILURE -> {
                log.warn("NotificationSenderSimulator: simulating transient failure for {} on attempt {}", identifier, attempt);
                throw new NotificationDeliveryException("Simulated transient network failure connecting to provider for " + identifier);
            }
            case TERMINAL_FAILURE -> {
                log.error("NotificationSenderSimulator: simulating terminal delivery failure for {} on attempt {}", identifier, attempt);
                throw new NotificationDeliveryException("Simulated terminal delivery failure: invalid recipient or unrouteable domain", true);
            }
            default -> true;
        };
    }
}
