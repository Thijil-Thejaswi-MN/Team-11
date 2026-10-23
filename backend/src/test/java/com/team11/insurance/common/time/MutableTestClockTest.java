package com.team11.insurance.common.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-05 MutableTestClock Unit Tests")
class MutableTestClockTest {

    private MutableTestClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableTestClock();
    }

    @Test
    @DisplayName("Virtual time can be set to a fixed instant")
    void testSetFixedInstant() {
        Instant target = Instant.parse("2026-11-01T10:00:00Z");
        clock.setFixed(target);

        assertEquals(target, clock.instant());
        assertEquals(Duration.ZERO, clock.getOffset());
    }

    @Test
    @DisplayName("Virtual time can be advanced relatively")
    void testAdvanceTime() {
        Instant base = Instant.parse("2026-10-20T10:00:00Z");
        clock.setFixed(base);

        clock.advance(Duration.ofDays(15));
        assertEquals(base.plus(15, ChronoUnit.DAYS), clock.instant());
        assertEquals(15 * 86400, clock.getOffset().getSeconds());

        clock.advance(Duration.ofHours(2));
        assertEquals(base.plus(15, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS), clock.instant());
    }

    @Test
    @DisplayName("Resetting time clears offsets and reverts to system time base")
    void testResetTime() {
        Instant target = Instant.parse("2026-11-01T10:00:00Z");
        clock.setFixed(target);
        clock.advance(Duration.ofDays(5));

        clock.reset();
        assertEquals(Duration.ZERO, clock.getOffset());
        // Verify clock is advancing with real time (approx within 2 seconds of now)
        long diff = Math.abs(Instant.now().getEpochSecond() - clock.instant().getEpochSecond());
        assertTrue(diff < 2, "Reset clock should track system time");
    }

    @Test
    @DisplayName("CRITICAL: withZone() preserves virtual offset and shared mutation behavior")
    void testWithZonePreservesVirtualOffset() {
        Instant base = Instant.parse("2026-10-20T12:00:00Z");
        clock.setFixed(base);
        clock.advance(Duration.ofDays(10));

        ZoneId utc = ZoneId.of("UTC");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");

        MutableTestClock utcClock = clock.withZone(utc);
        MutableTestClock tokyoClock = clock.withZone(tokyo);

        assertEquals(utc, utcClock.getZone());
        assertEquals(tokyo, tokyoClock.getZone());

        Instant expected = base.plus(10, ChronoUnit.DAYS);
        assertEquals(expected, utcClock.instant());
        assertEquals(expected, tokyoClock.instant());

        // LocalDate respects the timezone offset (+9 hours for Tokyo)
        LocalDate utcDate = LocalDate.ofInstant(utcClock.instant(), utc);
        LocalDate tokyoDate = LocalDate.ofInstant(tokyoClock.instant(), tokyo);
        assertEquals(LocalDate.parse("2026-10-30"), utcDate);
        assertEquals(LocalDate.parse("2026-10-30"), tokyoDate);

        // Advancing the base clock reflects in the zoned clocks
        clock.advance(Duration.ofDays(2));
        assertEquals(base.plus(12, ChronoUnit.DAYS), utcClock.instant());
        assertEquals(base.plus(12, ChronoUnit.DAYS), tokyoClock.instant());
    }

    @Test
    @DisplayName("Deterministic time-dependent calculations using injected clock")
    void testDeterministicTimeCalculations() {
        clock.setFixed(Instant.parse("2026-10-01T00:00:00Z"));
        LocalDate startDate = LocalDate.ofInstant(clock.instant(), ZoneId.of("UTC"));

        clock.advance(Duration.ofDays(14));
        LocalDate day14 = LocalDate.ofInstant(clock.instant(), ZoneId.of("UTC"));
        assertEquals(14, ChronoUnit.DAYS.between(startDate, day14));

        clock.advance(Duration.ofDays(1));
        LocalDate day15 = LocalDate.ofInstant(clock.instant(), ZoneId.of("UTC"));
        assertEquals(15, ChronoUnit.DAYS.between(startDate, day15));
    }
}
