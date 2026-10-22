package com.team11.insurance.common.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

/**
 * Controller for manipulating virtual time during development and testing.
 * <p>
 * Work Item: P4-05
 * Owner: Person 4 (Raghunandan Kotary)
 * <p>
 * Effective endpoints (with context-path /api/v1):
 * <ul>
 *   <li>POST /api/v1/test/clock</li>
 *   <li>GET /api/v1/test/clock</li>
 * </ul>
 * Guarded strictly under dev and test profiles. Throws {@link IllegalStateException}
 * on startup if active profiles include 'prod' or 'production'.
 */
@RestController
@RequestMapping("/test/clock")
@Profile({"dev", "test"})
public class TestClockController {

    private static final Logger log = LoggerFactory.getLogger(TestClockController.class);

    private final MutableTestClock clock;
    private final Environment environment;

    public TestClockController(MutableTestClock clock, Environment environment) {
        this.clock = clock;
        this.environment = environment;
    }

    @PostConstruct
    public void validateProfileIsolation() {
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = Arrays.stream(activeProfiles)
                    .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: TestClockController cannot be enabled in production profile!");
            }
        }
        log.warn("TestClockController is ACTIVE for dev/test profiles at /test/clock.");
    }

    @GetMapping
    public ResponseEntity<ClockControlResponse> getCurrentVirtualTime() {
        Instant current = clock.instant();
        ZoneId zone = clock.getZone();
        LocalDate date = LocalDate.ofInstant(current, zone);
        long offsetSeconds = clock.getOffset().getSeconds();

        return ResponseEntity.ok(ClockControlResponse.success("GET", current, date, offsetSeconds));
    }

    @PostMapping
    public ResponseEntity<ClockControlResponse> controlClock(@RequestBody ClockControlRequest request) {
        if (request == null || request.action() == null) {
            return ResponseEntity.badRequest().build();
        }

        switch (request.action()) {
            case SET -> {
                if (request.isoInstant() == null || request.isoInstant().isBlank()) {
                    return ResponseEntity.badRequest().build();
                }
                Instant fixedInstant = Instant.parse(request.isoInstant());
                clock.setFixed(fixedInstant);
                log.info("TestClockController: Virtual clock SET to {}", fixedInstant);
            }
            case ADVANCE -> {
                long amount = request.amount() != null ? request.amount() : 1;
                ChronoUnit unit = request.unit() != null ? request.unit() : ChronoUnit.DAYS;
                Duration duration = Duration.of(amount, unit);
                clock.advance(duration);
                log.info("TestClockController: Virtual clock ADVANCED by {} {}", amount, unit);
            }
            case RESET -> {
                clock.reset();
                log.info("TestClockController: Virtual clock RESET to system time.");
            }
        }

        Instant current = clock.instant();
        ZoneId zone = clock.getZone();
        LocalDate date = LocalDate.ofInstant(current, zone);
        long offsetSeconds = clock.getOffset().getSeconds();

        return ResponseEntity.ok(ClockControlResponse.success(request.action().name(), current, date, offsetSeconds));
    }
}
