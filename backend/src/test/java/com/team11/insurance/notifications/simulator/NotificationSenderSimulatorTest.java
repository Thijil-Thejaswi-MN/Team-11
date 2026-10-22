package com.team11.insurance.notifications.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-05 NotificationSenderSimulator Unit Tests")
class NotificationSenderSimulatorTest {

    private NotificationSenderSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new NotificationSenderSimulator();
    }

    @Test
    @DisplayName("SUCCESS mode returns true and tracks attempts")
    void testSuccessMode() throws Exception {
        boolean delivered = simulator.simulateDelivery("notif-001", "EMAIL", "asha@example.test");
        assertTrue(delivered);
        assertEquals(1, simulator.getAttempts("notif-001"));
    }

    @Test
    @DisplayName("TRANSIENT_FAILURE mode throws non-terminal delivery exception")
    void testTransientFailureMode() {
        simulator.setMode(NotificationSenderSimulator.DeliveryMode.TRANSIENT_FAILURE);

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class, () ->
                simulator.simulateDelivery("notif-002", "SMS", "+919876543210"));

        assertFalse(ex.isTerminal());
        assertTrue(ex.getMessage().contains("transient"));
        assertEquals(1, simulator.getAttempts("notif-002"));
    }

    @Test
    @DisplayName("TERMINAL_FAILURE mode throws terminal delivery exception for dead-letter handling")
    void testTerminalFailureMode() {
        simulator.setMode(NotificationSenderSimulator.DeliveryMode.TERMINAL_FAILURE);

        NotificationDeliveryException ex = assertThrows(NotificationDeliveryException.class, () ->
                simulator.simulateDelivery("notif-003", "EMAIL", "invalid-domain@bad.test"));

        assertTrue(ex.isTerminal());
        assertEquals(1, simulator.getAttempts("notif-003"));
    }

    @Test
    @DisplayName("failNTimesThenSucceed enables deterministic retry-until-success testing")
    void testFailNTimesThenSucceed() throws Exception {
        simulator.setFailNTimesThenSucceed("retry-user-1", 2);

        // Attempt 1 fails
        assertThrows(NotificationDeliveryException.class, () ->
                simulator.simulateDelivery("retry-user-1", "EMAIL", "user@test.org"));
        assertEquals(1, simulator.getAttempts("retry-user-1"));

        // Attempt 2 fails
        assertThrows(NotificationDeliveryException.class, () ->
                simulator.simulateDelivery("retry-user-1", "EMAIL", "user@test.org"));
        assertEquals(2, simulator.getAttempts("retry-user-1"));

        // Attempt 3 succeeds
        boolean result = simulator.simulateDelivery("retry-user-1", "EMAIL", "user@test.org");
        assertTrue(result);
        assertEquals(3, simulator.getAttempts("retry-user-1"));
    }
}
