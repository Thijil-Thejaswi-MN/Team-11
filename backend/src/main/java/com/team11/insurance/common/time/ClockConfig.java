package com.team11.insurance.common.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Spring configuration registering the application Clock bean based on active profiles.
 * <p>
 * Work Item: P4-05
 * Owner: Person 4 (Raghunandan Kotary)
 */
@Configuration
public class ClockConfig {

    @Bean
    @Primary
    @Profile("!dev & !test")
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Primary
    @Profile({"dev", "test"})
    public MutableTestClock mutableTestClock() {
        return new MutableTestClock();
    }
}
