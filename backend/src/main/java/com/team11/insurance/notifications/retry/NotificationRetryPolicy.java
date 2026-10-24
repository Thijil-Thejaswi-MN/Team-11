package com.team11.insurance.notifications.retry;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Retry Policy implementing the P4 Contract (Section 4) & SAD §4.1.7 backoff schedule.
 * <p>
 * <b>Contract Discrepancy & Interpretation:</b>
 * The P4 contract Section 4.3 contains two related specifications:
 * <ul>
 *   <li>"Transient network/gateway faults retry up to 3 times with exponential backoff (2s, 10s, 30s)."</li>
 *   <li>"On 3 consecutive failures, status moves to FAILED in the dead-letter log."</li>
 * </ul>
 * <i>Discrepancy:</i> If "3 consecutive failures" terminated the lifecycle after attempt 3 (initial attempt + 2 retries),
 * the third backoff interval (30s) would be unreachable and dead code.
 * <p>
 * <i>Chosen Interpretation:</i> To honor all three contract-defined backoff intervals (2s, 10s, 30s),
 * the system allows an initial dispatch (attempt 1) followed by up to 3 retries (attempts 2, 3, 4 with delays of 2s, 10s, 30s).
 * Upon failure of the 3rd retry (attempt 4, representing 3 consecutive retried failures after the initial fault),
 * the record is transitioned to {@code FAILED} in the dead-letter log.
 * Terminal/non-retryable errors transition immediately to {@code FAILED} without consuming retries.
 */
@Component
public class NotificationRetryPolicy {

    /**
     * Contract backoff intervals in seconds: [2, 10, 30].
     */
    public static final List<Long> BACKOFF_SCHEDULE_SECONDS = List.of(2L, 10L, 30L);

    public static final int MAX_RETRIES = 3;

    private final Clock clock;

    public NotificationRetryPolicy() {
        this(Clock.systemUTC());
    }

    public NotificationRetryPolicy(Clock clock) {
        this.clock = clock;
    }

    /**
     * Determines whether another delivery attempt should be made.
     *
     * @param attemptCount the total attempts made so far (1 = initial attempt, 2 = retry 1, etc.)
     * @param terminal     true if the error was classified as non-retryable/terminal
     * @return true if retry is permissible
     */
    public boolean shouldRetry(int attemptCount, boolean terminal) {
        if (terminal) {
            return false;
        }
        // Initial attempt is attempt 1. Up to 3 retries allowed (attempts 2, 3, 4).
        return attemptCount <= MAX_RETRIES;
    }

    /**
     * Computes the scheduled timestamp for the next retry attempt based on backoff intervals.
     *
     * @param attemptCount the total attempts made so far
     * @return the instant when the next retry is due
     */
    public Instant calculateNextRetry(int attemptCount) {
        return calculateNextRetry(attemptCount, clock.instant());
    }

    /**
     * Computes the scheduled timestamp for the next retry attempt relative to a given instant.
     *
     * @param attemptCount the total attempts made so far
     * @param fromTime     reference timestamp
     * @return the instant when the next retry is due
     */
    public Instant calculateNextRetry(int attemptCount, Instant fromTime) {
        long delaySeconds = getBackoffSeconds(attemptCount);
        return fromTime.plusSeconds(delaySeconds);
    }

    /**
     * Returns the backoff duration in seconds for the given attempt.
     *
     * @param attemptCount the total attempts made so far (1-indexed)
     * @return delay in seconds (2s for attempt 1, 10s for attempt 2, 30s for attempt 3+)
     */
    public long getBackoffSeconds(int attemptCount) {
        if (attemptCount <= 1) {
            return BACKOFF_SCHEDULE_SECONDS.get(0); // 2s
        } else if (attemptCount == 2) {
            return BACKOFF_SCHEDULE_SECONDS.get(1); // 10s
        } else {
            return BACKOFF_SCHEDULE_SECONDS.get(2); // 30s
        }
    }

    public int getMaxRetries() {
        return MAX_RETRIES;
    }
}
