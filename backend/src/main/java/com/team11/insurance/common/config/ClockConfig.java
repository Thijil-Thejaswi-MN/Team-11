package com.team11.insurance.common.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.team11.insurance.common.time.MutableTestClock;

/**
 * Spring configuration providing injectable {@link Clock} beans based on active profiles.
 * <p>
 * - In production (no dev/test profile): supplies {@link Clock#systemDefaultZone()}.
 * - In development/testing (dev or test profiles): supplies {@link MutableTestClock}.
 * Allows tests to supply fixed or mutable virtual clock without hard-coding
 * system time dependencies.
 * <p>
 * Work Item: P4-05
 * Owner: Person 4 (Raghunandan Kotary)
 */
@Configuration
public class ClockConfig {

    @Bean
    @Profile("!dev & !test")
    @ConditionalOnMissingBean(Clock.class)
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile({"dev", "test"})
    @ConditionalOnMissingBean(MutableTestClock.class)
    public MutableTestClock mutableTestClock() {
        return new MutableTestClock();
    }
}

