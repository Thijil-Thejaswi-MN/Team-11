package com.team11.insurance.payments.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-05 PaymentGatewaySimulator Unit Tests")
class PaymentGatewaySimulatorTest {

    private PaymentGatewaySimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new PaymentGatewaySimulator();
    }

    @Test
    @DisplayName("SUCCESS mode returns transaction reference and increments attempt count")
    void testSuccessMode() throws Exception {
        PaymentGatewaySimulator.GatewayResponse resp = simulator.processPayment("app-7001-1", new BigDecimal("8500.00"));

        assertTrue(resp.successful());
        assertNotNull(resp.transactionReference());
        assertTrue(resp.transactionReference().startsWith("TXN-"));
        assertEquals(1, simulator.getAttemptsForKey("app-7001-1"));
    }

    @Test
    @DisplayName("DECLINED mode returns business failure response without throwing exception")
    void testDeclinedMode() throws Exception {
        simulator.setMode(PaymentGatewaySimulator.SimulatorMode.DECLINED);
        PaymentGatewaySimulator.GatewayResponse resp = simulator.processPayment("app-7002-1", new BigDecimal("10000.00"));

        assertFalse(resp.successful());
        assertEquals("PAY-402", resp.errorCode());
        assertTrue(resp.errorMessage().contains("Card declined"));
    }

    @Test
    @DisplayName("TIMEOUT mode throws PaymentTimeoutException to enable retry testing")
    void testTimeoutMode() {
        simulator.setMode(PaymentGatewaySimulator.SimulatorMode.TIMEOUT);

        PaymentTimeoutException ex = assertThrows(PaymentTimeoutException.class, () ->
                simulator.processPayment("app-7003-1", new BigDecimal("5000.00")));

        assertTrue(ex.getMessage().contains("Gateway read timeout"));
        assertEquals(1, simulator.getAttemptsForKey("app-7003-1"));
    }

    @Test
    @DisplayName("GATEWAY_ERROR mode returns 500 error response")
    void testGatewayErrorMode() throws Exception {
        simulator.setMode(PaymentGatewaySimulator.SimulatorMode.GATEWAY_ERROR);
        PaymentGatewaySimulator.GatewayResponse resp = simulator.processPayment("app-7004-1", new BigDecimal("2500.00"));

        assertFalse(resp.successful());
        assertEquals("SYS-500", resp.errorCode());
    }

    @Test
    @DisplayName("Per-key mode override isolates simulated behavior for specific test cases")
    void testPerKeyOverride() throws Exception {
        simulator.setMode(PaymentGatewaySimulator.SimulatorMode.SUCCESS);
        simulator.setModeForKey("retry-key", PaymentGatewaySimulator.SimulatorMode.TIMEOUT);

        assertThrows(PaymentTimeoutException.class, () ->
                simulator.processPayment("retry-key", new BigDecimal("1200.00")));

        PaymentGatewaySimulator.GatewayResponse defaultResp =
                simulator.processPayment("normal-key", new BigDecimal("1200.00"));
        assertTrue(defaultResp.successful());
    }

    @Test
    @DisplayName("setTimeoutAttempts: simulates transient timeout attempts followed by success")
    void testTransientTimeoutSequence() throws Exception {
        simulator.setTimeoutAttempts(2);

        // Attempt 1: timeout
        assertThrows(PaymentTimeoutException.class, () ->
                simulator.processPayment("seq-key", new BigDecimal("1000.00")));
        assertEquals(1, simulator.getAttemptsForKey("seq-key"));

        // Attempt 2: timeout
        assertThrows(PaymentTimeoutException.class, () ->
                simulator.processPayment("seq-key", new BigDecimal("1000.00")));
        assertEquals(2, simulator.getAttemptsForKey("seq-key"));

        // Attempt 3: recovered to SUCCESS
        PaymentGatewaySimulator.GatewayResponse resp =
                simulator.processPayment("seq-key", new BigDecimal("1000.00"));
        assertTrue(resp.successful());
        assertEquals(3, simulator.getAttemptsForKey("seq-key"));
    }

    @Test
    @DisplayName("NETWORK_DISCONNECT mode throws PaymentTimeoutException indicating network failure")
    void testNetworkDisconnectMode() {
        simulator.setMode(PaymentGatewaySimulator.SimulatorMode.NETWORK_DISCONNECT);

        PaymentTimeoutException ex = assertThrows(PaymentTimeoutException.class, () ->
                simulator.processPayment("disc-key", new BigDecimal("1500.00")));

        assertTrue(ex.getMessage().contains("Network disconnect"));
        assertEquals(1, simulator.getAttemptsForKey("disc-key"));
    }
}
