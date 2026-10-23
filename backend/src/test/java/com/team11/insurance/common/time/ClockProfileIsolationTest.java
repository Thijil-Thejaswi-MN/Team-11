package com.team11.insurance.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-05 Clock Profile Isolation and Security Tests")
class ClockProfileIsolationTest {

    @Test
    @DisplayName("CRITICAL: TestClockController MUST fail startup if 'prod' profile is active")
    void testControllerFailsInProdProfile() {
        MockEnvironment envProd = new MockEnvironment();
        envProd.setActiveProfiles("prod");

        MutableTestClock clock = new MutableTestClock();
        TestClockController controller = new TestClockController(clock, envProd);

        IllegalStateException ex = assertThrows(IllegalStateException.class, controller::validateProfileIsolation);
        assertTrue(ex.getMessage().contains("TestClockController cannot be enabled in production profile"));
    }

    @Test
    @DisplayName("CRITICAL: TestClockController MUST fail startup if 'production' profile is active")
    void testControllerFailsInProductionProfile() {
        MockEnvironment envProd = new MockEnvironment();
        envProd.setActiveProfiles("production");

        MutableTestClock clock = new MutableTestClock();
        TestClockController controller = new TestClockController(clock, envProd);

        IllegalStateException ex = assertThrows(IllegalStateException.class, controller::validateProfileIsolation);
        assertTrue(ex.getMessage().contains("TestClockController cannot be enabled in production profile"));
    }

    @Test
    @DisplayName("TestClockController initializes cleanly under 'dev' profile")
    void testControllerAllowedInDevProfile() {
        MockEnvironment envDev = new MockEnvironment();
        envDev.setActiveProfiles("dev");

        MutableTestClock clock = new MutableTestClock();
        TestClockController controller = new TestClockController(clock, envDev);

        assertDoesNotThrow(controller::validateProfileIsolation);
    }

    @Test
    @DisplayName("TestClockController initializes cleanly under 'test' profile")
    void testControllerAllowedInTestProfile() {
        MockEnvironment envTest = new MockEnvironment();
        envTest.setActiveProfiles("test");

        MutableTestClock clock = new MutableTestClock();
        TestClockController controller = new TestClockController(clock, envTest);

        assertDoesNotThrow(controller::validateProfileIsolation);
    }
}
