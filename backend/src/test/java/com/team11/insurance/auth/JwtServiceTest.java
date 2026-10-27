package com.team11.insurance.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.service.JwtService;
import com.team11.insurance.common.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-05 INS-SR-003 / INS-SR-005: JwtService HMAC-SHA256 Token Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private MockEnvironment environment;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        objectMapper = new ObjectMapper();
        jwtService = new JwtService(environment, objectMapper);
        jwtService.validateConfiguration();
    }

    @Test
    @DisplayName("Generate and parse valid JWT token with claims")
    void testGenerateAndParseValidToken() {
        Instant now = Instant.now();
        String token = jwtService.generateToken(1001L, "customer@test.invalid", "CUSTOMER", null, "sess-123", now);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT must contain three parts separated by dots");

        Map<String, Object> claims = jwtService.parseAndValidateToken(token, now);
        assertEquals(JwtService.ISSUER, claims.get("iss"));
        assertEquals("1001", claims.get("sub"));
        assertEquals("customer@test.invalid", claims.get("email"));
        assertEquals("CUSTOMER", claims.get("role"));
        assertEquals("sess-123", claims.get("sessionId"));
        assertNull(claims.get("insurerId"));
    }

    @Test
    @DisplayName("Generate and parse token for staff user with insurerId")
    void testStaffTokenContainsInsurerId() {
        Instant now = Instant.now();
        String token = jwtService.generateToken(2001L, "underwriter@insurer.invalid", "UNDERWRITER", 5L, "sess-456", now);

        Map<String, Object> claims = jwtService.parseAndValidateToken(token, now);
        assertEquals("2001", claims.get("sub"));
        assertEquals("UNDERWRITER", claims.get("role"));
        assertEquals(5, ((Number) claims.get("insurerId")).longValue());
    }

    @Test
    @DisplayName("Expired token is rejected with AuthenticationException")
    void testExpiredTokenThrows() {
        Instant past = Instant.now().minusSeconds(30000);
        String token = jwtService.generateToken(1001L, "cust@test.invalid", "CUSTOMER", null, "sess-999", past);

        Instant now = Instant.now();
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> jwtService.parseAndValidateToken(token, now));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Tampered payload signature verification failure throws AuthenticationException")
    void testTamperedPayloadThrows() {
        Instant now = Instant.now();
        String token = jwtService.generateToken(1001L, "cust@test.invalid", "CUSTOMER", null, "sess-001", now);

        String[] parts = token.split("\\.");
        // Modify middle segment (payload)
        String tamperedPayload = parts[1] + "tamper";
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(AuthenticationException.class,
                () -> jwtService.parseAndValidateToken(tamperedToken, now));
    }

    @Test
    @DisplayName("Tampered signature fails verification")
    void testTamperedSignatureThrows() {
        Instant now = Instant.now();
        String token = jwtService.generateToken(1001L, "cust@test.invalid", "CUSTOMER", null, "sess-001", now);

        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidSignature1234567890";

        assertThrows(AuthenticationException.class,
                () -> jwtService.parseAndValidateToken(tamperedToken, now));
    }

    @Test
    @DisplayName("CRITICAL: Production profile fails startup if JWT secret is missing or insecure")
    void testProductionFailsWithoutSecureSecret() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        JwtService prodJwtService = new JwtService(prodEnv, objectMapper);
        IllegalStateException ex = assertThrows(IllegalStateException.class, prodJwtService::validateConfiguration);
        assertTrue(ex.getMessage().contains("CRITICAL SECURITY VIOLATION"));
    }
}
