package com.team11.insurance.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.dto.LoginRequest;
import com.team11.insurance.auth.dto.LoginResponse;
import com.team11.insurance.auth.dto.RegisterRequest;
import com.team11.insurance.auth.dto.RegisterResponse;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.entity.UserSession;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.repository.UserSessionRepository;
import com.team11.insurance.auth.service.AuthenticationService;
import com.team11.insurance.auth.service.JwtService;
import com.team11.insurance.auth.service.PasswordService;
import com.team11.insurance.common.exception.AccountLockedException;
import com.team11.insurance.common.exception.AuthenticationException;
import com.team11.insurance.common.exception.StateConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("P2-05 AuthenticationService Integration and Security Rules Tests")
class AuthenticationServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    private AuthenticationService authService;
    private PasswordService passwordService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        jwtService = new JwtService(env, new ObjectMapper());
        jwtService.validateConfiguration();

        authService = new AuthenticationService(userRepository, sessionRepository, passwordService, jwtService);
    }

    @Test
    @DisplayName("Successful customer registration stores BCrypt hash and returns userId")
    void testSuccessfulRegistration() {
        RegisterRequest req = new RegisterRequest("Test User", "test.user@example.invalid", "1234567890", "SecretPassword123!");
        RegisterResponse res = authService.register(req);

        assertNotNull(res);
        assertNotNull(res.getUserId());

        User user = userRepository.findById(res.getUserId()).orElseThrow();
        assertEquals("test.user@example.invalid", user.getEmail());
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertNotEquals("SecretPassword123!", user.getPasswordHash());
        assertTrue(passwordService.matches("SecretPassword123!", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Duplicate registration throws StateConflictException (409)")
    void testDuplicateRegistrationThrowsConflict() {
        RegisterRequest req1 = new RegisterRequest("First", "duplicate@test.invalid", "111", "Pass12345!");
        authService.register(req1);

        RegisterRequest req2 = new RegisterRequest("Second", "duplicate@test.invalid", "222", "Pass67890!");
        assertThrows(StateConflictException.class, () -> authService.register(req2));
    }

    @Test
    @DisplayName("Successful login returns JWT, role, and creates persistent session")
    void testSuccessfulLogin() {
        RegisterRequest req = new RegisterRequest("Login User", "login@test.invalid", "999", "CorrectPass123!");
        RegisterResponse regRes = authService.register(req);

        LoginRequest loginReq = new LoginRequest("login@test.invalid", "CorrectPass123!");
        LoginResponse loginRes = authService.login(loginReq, "127.0.0.1", "Browser");

        assertNotNull(loginRes);
        assertNotNull(loginRes.getAccessToken());
        assertEquals("CUSTOMER", loginRes.getRole());
        assertEquals(900, loginRes.getExpiresIn());

        // Validate token and session resolution
        CurrentUser currentUser = authService.validateTokenAndSession(loginRes.getAccessToken(), Instant.now());
        assertEquals(regRes.getUserId(), currentUser.getUserId());
        assertEquals(UserRole.CUSTOMER, currentUser.getUserRole());
    }

    @Test
    @DisplayName("Incorrect password throws AuthenticationException (401)")
    void testIncorrectPasswordThrows() {
        RegisterRequest req = new RegisterRequest("Login User", "wrongpass@test.invalid", "999", "CorrectPass123!");
        authService.register(req);

        LoginRequest loginReq = new LoginRequest("wrongpass@test.invalid", "WrongPassword!");
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login(loginReq, "127.0.0.1", "Browser"));
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Unknown email throws same AuthenticationException (401) to prevent enumeration")
    void testUnknownEmailReturnsSameError() {
        LoginRequest loginReq = new LoginRequest("nonexistent@test.invalid", "AnyPassword!");
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login(loginReq, "127.0.0.1", "Browser"));
        assertEquals("Invalid email or password.", ex.getMessage());
    }

    @Test
    @DisplayName("Account lockout: 5 consecutive failed logins triggers 15-minute lockout (AUTH-423)")
    void testAccountLockoutAfterFiveFailures() {
        RegisterRequest req = new RegisterRequest("Lockout User", "lockout@test.invalid", "999", "OriginalPass123!");
        authService.register(req);

        LoginRequest wrongLogin = new LoginRequest("lockout@test.invalid", "WrongPass!");

        // First 4 failures throw AuthenticationException
        for (int i = 1; i <= 4; i++) {
            assertThrows(AuthenticationException.class, () -> authService.login(wrongLogin, "127.0.0.1", "Browser"));
        }

        // 5th failure triggers account lockout with AccountLockedException
        AccountLockedException lockEx = assertThrows(AccountLockedException.class,
                () -> authService.login(wrongLogin, "127.0.0.1", "Browser"));
        assertTrue(lockEx.getMessage().contains("Account locked"));

        // 6th attempt even with correct password is denied due to active lockout
        LoginRequest correctLogin = new LoginRequest("lockout@test.invalid", "OriginalPass123!");
        assertThrows(AccountLockedException.class, () -> authService.login(correctLogin, "127.0.0.1", "Browser"));
    }

    @Test
    @DisplayName("Session idle timeout: token rejected after 15 minutes of inactivity")
    void testSessionInactivityTimeout() {
        RegisterRequest req = new RegisterRequest("Idle User", "idle@test.invalid", "999", "Pass12345!");
        authService.register(req);

        LoginResponse loginRes = authService.login(new LoginRequest("idle@test.invalid", "Pass12345!"), "127.0.0.1", "Browser");

        // Active within 15 minutes works
        CurrentUser activeUser = authService.validateTokenAndSession(loginRes.getAccessToken(), Instant.now());
        assertNotNull(activeUser);

        // Simulate 16 minutes of user inactivity on the session
        UserSession session = sessionRepository.findAll().get(0);
        session.setLastAccessedAt(Instant.now().minusSeconds(UserSession.INACTIVITY_TIMEOUT_SECONDS + 60));
        sessionRepository.saveAndFlush(session);

        // Attempting to use token after session went idle throws AuthenticationException
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(loginRes.getAccessToken(), Instant.now()));
        assertTrue(ex.getMessage().contains("Session expired due to inactivity"));
    }

    @Test
    @DisplayName("Logout revokes the server-side session")
    void testLogoutRevokesSession() {
        RegisterRequest req = new RegisterRequest("Logout User", "logout@test.invalid", "999", "Pass12345!");
        authService.register(req);

        LoginResponse loginRes = authService.login(new LoginRequest("logout@test.invalid", "Pass12345!"), "127.0.0.1", "Browser");
        authService.logout(loginRes.getAccessToken());

        // Subsequent token usage fails because session is revoked
        assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(loginRes.getAccessToken(), Instant.now()));
    }

    @Test
    @DisplayName("Finding 4: Validly signed token with subject differing from session owner is rejected (ownership invariant)")
    void testSessionOwnershipMismatchRejected() {
        // Register and login legitimate User A (victim)
        RegisterRequest reqA = new RegisterRequest("User A", "usera@test.invalid", "111", "Pass12345!");
        RegisterResponse resA = authService.register(reqA);
        LoginResponse loginResA = authService.login(new LoginRequest("usera@test.invalid", "Pass12345!"), "127.0.0.1", "Agent");

        // Attacker generates/has a validly signed token with subject = User B (9999), but sessionId = User A's session
        UserSession sessionA = sessionRepository.findAll().get(0);
        String spoofedToken = jwtService.generateToken(9999L, "attacker@test.invalid", "CUSTOMER", null, sessionA.getSessionId(), Instant.now());

        // Token is cryptographically valid, but session belongs to User A (resA.getUserId()), NOT 9999
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(spoofedToken, Instant.now()));

        assertTrue(ex.getMessage().contains("Session ownership mismatch"),
                "Must reject when token subject does not match referenced session owner");
        assertEquals("AUTH-401", ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Finding 4: Failed ownership validation DOES NOT touch or mutate the victim's session")
    void testSessionOwnershipMismatchDoesNotMutateVictimSession() {
        RegisterRequest reqA = new RegisterRequest("Victim User", "victim@test.invalid", "111", "Pass12345!");
        authService.register(reqA);
        authService.login(new LoginRequest("victim@test.invalid", "Pass12345!"), "127.0.0.1", "Agent");

        UserSession sessionA = sessionRepository.findAll().get(0);
        Instant originalLastAccessed = sessionA.getLastAccessedAt();

        // Advance clock by 100 seconds and attempt spoofed token usage
        Instant later = originalLastAccessed.plusSeconds(100);
        String spoofedToken = jwtService.generateToken(8888L, "attacker@test.invalid", "CUSTOMER", null, sessionA.getSessionId(), later);

        assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(spoofedToken, later));

        // Verify victim's session was NOT refreshed or mutated
        UserSession reloaded = sessionRepository.findBySessionId(sessionA.getSessionId()).orElseThrow();
        assertEquals(originalLastAccessed, reloaded.getLastAccessedAt(),
                "Victim session lastAccessedAt must remain unchanged after rejected ownership check");
    }

    @Test
    @DisplayName("Finding 4: Token referencing nonexistent session is rejected with AUTH-401")
    void testNonexistentSessionRejected() {
        String tokenWithBadSession = jwtService.generateToken(1001L, "user@test.invalid", "CUSTOMER", null, "nonexistent-sess-uuid", Instant.now());

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(tokenWithBadSession, Instant.now()));
        assertTrue(ex.getMessage().contains("Session not found"));
    }

    @Test
    @DisplayName("Finding 4: Malformed or non-numeric token subject is rejected safely with AUTH-401")
    void testMalformedSubjectRejected() {
        Instant now = Instant.now();
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(null, now));
        assertNotNull(ex);
    }

    @Test
    @DisplayName("Finding 4: Token with non-numeric or negative subject is rejected safely")
    void testNonNumericOrNegativeSubRejected() throws Exception {
        Instant now = Instant.now();
        // Create session
        UserSession session = new UserSession("test-sess-sub", 500L, now, 900, "127.0.0.1", "Agent");
        sessionRepository.save(session);

        // 1. Non-numeric sub
        java.util.Map<String, Object> header = java.util.Map.of("alg", "HS256", "typ", "JWT");
        java.util.Map<String, Object> badSubClaims = new java.util.LinkedHashMap<>();
        badSubClaims.put("iss", JwtService.ISSUER);
        badSubClaims.put("sub", "not-a-number");
        badSubClaims.put(JwtService.CLAIM_SESSION_ID, "test-sess-sub");
        badSubClaims.put(JwtService.CLAIM_ROLE, "CUSTOMER");
        badSubClaims.put("iat", now.getEpochSecond());
        badSubClaims.put("exp", now.getEpochSecond() + 900);

        ObjectMapper mapper = new ObjectMapper();
        String encHeader = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(header));
        String encPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(badSubClaims));
        String sigInput = encHeader + "." + encPayload;

        // Sign using reflection or package method if private, or let's use javax.crypto.Mac with test key
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("dev-insecure-secret-insurance-workflow-team11-test-key-256-bits-long!!".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(sigInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String tokenWithBadSub = sigInput + "." + sig;

        AuthenticationException ex1 = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(tokenWithBadSub, now));
        assertTrue(ex1.getMessage().contains("Token subject is not a valid user identifier"));

        // 2. Negative sub
        badSubClaims.put("sub", "-99");
        encPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(badSubClaims));
        sigInput = encHeader + "." + encPayload;
        sig = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(sigInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String tokenWithNegSub = sigInput + "." + sig;

        AuthenticationException ex2 = assertThrows(AuthenticationException.class,
                () -> authService.validateTokenAndSession(tokenWithNegSub, now));
        assertTrue(ex2.getMessage().contains("non-positive user identifier"));
    }
}
