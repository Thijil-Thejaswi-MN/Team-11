package com.team11.insurance.notifications;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.team11.insurance.notifications.retry.NotificationRetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("P4-08 Notification Retry Policy Tests")
class NotificationRetryPolicyTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-24T10:00:00Z");

    private NotificationRetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        retryPolicy = new NotificationRetryPolicy(fixedClock);
    }

    @Test
    @DisplayName("Backoff schedule adheres strictly to contract intervals: 2s, 10s, 30s")
    void testBackoffIntervals() {
        assertThat(retryPolicy.getBackoffSeconds(1)).isEqualTo(2L);
        assertThat(retryPolicy.getBackoffSeconds(2)).isEqualTo(10L);
        assertThat(retryPolicy.getBackoffSeconds(3)).isEqualTo(30L);
        assertThat(retryPolicy.getBackoffSeconds(4)).isEqualTo(30L);
    }

    @Test
    @DisplayName("Transient failures allow up to 3 retries (attempts 1, 2, 3)")
    void testTransientRetryDecisions() {
        // Attempt 1 fails -> allow retry 1 (attempt 2)
        assertThat(retryPolicy.shouldRetry(1, false)).isTrue();
        // Attempt 2 fails -> allow retry 2 (attempt 3)
        assertThat(retryPolicy.shouldRetry(2, false)).isTrue();
        // Attempt 3 fails -> allow retry 3 (attempt 4)
        assertThat(retryPolicy.shouldRetry(3, false)).isTrue();
        // Attempt 4 fails -> retries exhausted, transition to terminal failure
        assertThat(retryPolicy.shouldRetry(4, false)).isFalse();
    }

    @Test
    @DisplayName("Terminal failures are never retried")
    void testTerminalFailureNeverRetries() {
        assertThat(retryPolicy.shouldRetry(1, true)).isFalse();
        assertThat(retryPolicy.shouldRetry(2, true)).isFalse();
    }

    @Test
    @DisplayName("calculateNextRetry returns expected timestamp based on backoff delay")
    void testNextRetryCalculation() {
        Instant retry1 = retryPolicy.calculateNextRetry(1, FIXED_NOW);
        assertThat(retry1).isEqualTo(FIXED_NOW.plusSeconds(2));

        Instant retry2 = retryPolicy.calculateNextRetry(2, FIXED_NOW);
        assertThat(retry2).isEqualTo(FIXED_NOW.plusSeconds(10));

        Instant retry3 = retryPolicy.calculateNextRetry(3, FIXED_NOW);
        assertThat(retry3).isEqualTo(FIXED_NOW.plusSeconds(30));
    }
}
