package com.team11.insurance.common.time;

import java.time.temporal.ChronoUnit;

public record ClockControlRequest(
        ClockAction action,
        Long amount,
        ChronoUnit unit,
        String isoInstant
) {
    public enum ClockAction {
        SET,
        ADVANCE,
        RESET
    }
}
