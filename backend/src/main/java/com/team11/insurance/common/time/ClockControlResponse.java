package com.team11.insurance.common.time;

import java.time.Instant;
import java.time.LocalDate;

public record ClockControlResponse(
        String status,
        String action,
        Instant virtualInstant,
        LocalDate virtualDate,
        long offsetSeconds
) {
    public static ClockControlResponse success(String action, Instant instant, LocalDate date, long offsetSeconds) {
        return new ClockControlResponse("SUCCESS", action, instant, date, offsetSeconds);
    }
}
