package com.team11.insurance.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controllable virtual test clock implementing java.time.Clock.
 * <p>
 * Work Item: P4-05
 * Owner: Person 4 (Raghunandan Kotary)
 * <p>
 * Features:
 * <ul>
 *   <li>Active strictly in dev and test profiles.</li>
 *   <li>Supports setting explicit virtual instant, relative advancing, and resetting.</li>
 *   <li>Preserves virtual offset and mutable advance behavior across zones via {@link #withZone(ZoneId)}.</li>
 *   <li>Thread-safe state management.</li>
 * </ul>
 */
public class MutableTestClock extends Clock {

    private static class ClockState {
        final Clock baseClock;
        final Duration offset;

        ClockState(Clock baseClock, Duration offset) {
            this.baseClock = baseClock;
            this.offset = offset;
        }
    }

    private final AtomicReference<ClockState> state;
    private final ZoneId zone;

    public MutableTestClock() {
        this(Clock.systemDefaultZone(), Duration.ZERO);
    }

    public MutableTestClock(Clock delegate, Duration offset) {
        this.zone = delegate != null ? delegate.getZone() : ZoneId.systemDefault();
        Clock base = delegate != null ? delegate : Clock.systemDefaultZone();
        Duration off = offset != null ? offset : Duration.ZERO;
        this.state = new AtomicReference<>(new ClockState(base, off));
    }

    private MutableTestClock(AtomicReference<ClockState> sharedState, ZoneId zone) {
        this.state = sharedState;
        this.zone = zone != null ? zone : ZoneId.systemDefault();
    }

    public synchronized void setFixed(Instant fixedInstant, ZoneId zoneId) {
        ZoneId targetZone = zoneId != null ? zoneId : this.zone;
        this.state.set(new ClockState(Clock.fixed(fixedInstant, targetZone), Duration.ZERO));
    }

    public synchronized void setFixed(Instant fixedInstant) {
        setFixed(fixedInstant, this.zone);
    }

    public synchronized void advance(Duration duration) {
        if (duration == null) {
            return;
        }
        this.state.updateAndGet(current -> new ClockState(current.baseClock, current.offset.plus(duration)));
    }

    public synchronized void reset() {
        this.state.set(new ClockState(Clock.system(this.zone), Duration.ZERO));
    }

    public Duration getOffset() {
        return this.state.get().offset;
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public synchronized MutableTestClock withZone(ZoneId zone) {
        if (zone == null || zone.equals(this.zone)) {
            return this;
        }
        return new MutableTestClock(this.state, zone);
    }

    @Override
    public Instant instant() {
        ClockState current = this.state.get();
        return current.baseClock.instant().plus(current.offset);
    }
}
