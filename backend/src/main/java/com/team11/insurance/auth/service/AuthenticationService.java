package com.team11.insurance.auth.service;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.auth.dto.LoginRequest;
import com.team11.insurance.auth.dto.LoginResponse;
import com.team11.insurance.auth.dto.RegisterRequest;
import com.team11.insurance.auth.dto.RegisterResponse;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.entity.UserSession;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.repository.UserSessionRepository;
import com.team11.insurance.common.exception.AccountLockedException;
import com.team11.insurance.common.exception.AuthenticationException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Core authentication and session orchestration service (INS-F-001, INS-SR-002, INS-SR-003, INS-SR-005).
 */
@Service
@Transactional(noRollbackFor = {AuthenticationException.class, AccountLockedException.class})
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    public static final int MAX_FAILED_ATTEMPTS = 5; // Lockout after 5 consecutive failed logins
    public static final int LOCKOUT_DURATION_MINUTES = 15; // 15-minute lockout per SAD §4.3.1 & INS-SR-005

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthenticationService(UserRepository userRepository,
                                 UserSessionRepository sessionRepository,
                                 PasswordService passwordService,
                                 JwtService jwtService,
                                 ObjectProvider<Clock> clockProvider) {
        this(userRepository, sessionRepository, passwordService, jwtService,
                clockProvider != null ? clockProvider.getIfAvailable(Clock::systemDefaultZone) : Clock.systemDefaultZone());
    }

    public AuthenticationService(UserRepository userRepository,
                                 UserSessionRepository sessionRepository,
                                 PasswordService passwordService,
                                 JwtService jwtService,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public AuthenticationService(UserRepository userRepository,
                                 UserSessionRepository sessionRepository,
                                 PasswordService passwordService,
                                 JwtService jwtService) {
        this(userRepository, sessionRepository, passwordService, jwtService, (Clock) null);
    }

    /**
     * Registers a new customer account (INS-F-001).
     *
     * @param request validated registration details
     * @return response with created userId
     * @throws StateConflictException if email is already in use
     */
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new StateConflictException("User with email '" + normalizedEmail + "' already exists.");
        }

        // Salted BCrypt password hash (work factor 12)
        String passwordHash = passwordService.hashPassword(request.getPassword());

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                request.getPhone().trim(),
                passwordHash,
                UserRole.CUSTOMER,
                null
        );

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id={} role={}", savedUser.getId(), savedUser.getRole());

        return new RegisterResponse(savedUser.getId());
    }

    /**
     * Authenticates credentials, checks lockout, manages session, and issues JWT (INS-F-001, INS-SR-005).
     *
     * @param request credentials
     * @param ipAddress client IP
     * @param userAgent client browser / user agent
     * @return login response containing access token, role, and expiration
     * @throws AuthenticationException on invalid credentials
     * @throws AccountLockedException if account is locked due to 5 consecutive failures
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (userOpt.isEmpty()) {
            // Mitigate timing attacks by performing dummy BCrypt verification
            passwordService.matches(request.getPassword(), passwordService.getDummyHash());
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        // 1. Check account lockout status
        if (user.isAccountLocked(now)) {
            log.warn("Login attempt for locked account id={}", user.getId());
            throw new AccountLockedException("Account locked after 5 failed logins for 15 minutes.");
        }

        // 2. Clear expired lockout if time has passed
        if (user.getLockedUntil() != null && !now.isBefore(user.getLockedUntil())) {
            user.resetLoginLockout();
        }

        // 3. Verify password hash
        boolean passwordMatches = passwordService.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatches) {
            user.recordFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION_MINUTES);
            userRepository.saveAndFlush(user);

            if (user.isAccountLocked(now)) {
                log.warn("Account id={} reached 5 failed logins and is now locked for 15 minutes", user.getId());
                throw new AccountLockedException("Account locked after 5 failed logins for 15 minutes.");
            }

            throw new AuthenticationException("Invalid email or password.");
        }

        // 4. Successful login: reset failed attempt counters
        user.resetLoginLockout();
        userRepository.save(user);

        // 5. Create server-side session
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(
                sessionId,
                user.getId(),
                now,
                jwtService.getExpirationSeconds(),
                ipAddress,
                userAgent
        );
        sessionRepository.save(session);

        // 6. Issue signed HS256 JWT
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getValue(),
                user.getInsurerId(),
                sessionId,
                now
        );

        log.info("User id={} logged in successfully. Session created.", user.getId());

        return new LoginResponse(token, user.getRole().getValue(), UserSession.INACTIVITY_TIMEOUT_SECONDS);
    }

    /**
     * Validates an incoming bearer token and verifies the server-side session.
     *
     * @param token JWT token string
     * @param now current instant
     * @return authenticated {@link CurrentUser} context
     * @throws AuthenticationException if token is invalid, expired, or session is revoked/timed out
     */
    public CurrentUser validateTokenAndSession(String token, Instant now) {
        Instant effectiveNow = now != null ? now : clock.instant();
        Map<String, Object> claims = jwtService.parseAndValidateToken(token, effectiveNow);

        String sessionId = (String) claims.get(JwtService.CLAIM_SESSION_ID);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new AuthenticationException("Token missing session identifier");
        }

        String sub = (String) claims.get("sub");
        if (sub == null || sub.trim().isEmpty()) {
            throw new AuthenticationException("Token missing subject identifier");
        }

        Long userId;
        try {
            userId = Long.valueOf(sub.trim());
        } catch (NumberFormatException nfe) {
            throw new AuthenticationException("Token subject is not a valid user identifier");
        }

        if (userId <= 0) {
            throw new AuthenticationException("Token subject contains an invalid non-positive user identifier");
        }

        // 2 & 3. Retrieve referenced server-side session and verify it exists
        UserSession session = sessionRepository.findBySessionId(sessionId.trim())
                .orElseThrow(() -> new AuthenticationException("Session not found or has been terminated"));

        // 4. Verify session is valid, not revoked, not expired, and not beyond inactivity timeout
        if (!session.isValid(effectiveNow)) {
            session.revoke(effectiveNow);
            sessionRepository.save(session);
            throw new AuthenticationException("Session expired due to inactivity or revocation");
        }

        // 5 & 6. Enforce session-to-user ownership: verify session belongs to token subject
        if (session.getUserId() == null || !session.getUserId().equals(userId)) {
            log.warn("Security violation: session-to-user ownership mismatch. session.userId={}, token.sub={}",
                    session.getUserId(), userId);
            // Fail closed immediately. Do NOT touch or mutate the victim user's session!
            throw new AuthenticationException("Session ownership mismatch: token subject does not match session owner");
        }

        // 7. Only after ownership and session checks succeed, touch session to record activity
        session.touch(effectiveNow);
        sessionRepository.save(session);

        String role = (String) claims.get(JwtService.CLAIM_ROLE);
        Long insurerId = claims.get(JwtService.CLAIM_INSURER_ID) != null
                ? ((Number) claims.get(JwtService.CLAIM_INSURER_ID)).longValue()
                : null;

        return new CurrentUser(userId, role, insurerId);
    }

    /**
     * Logs out the user by revoking the server-side session associated with the token.
     *
     * @param token bearer token
     */
    public void logout(String token) {
        try {
            Instant now = clock.instant();
            Map<String, Object> claims = jwtService.parseAndValidateToken(token, now);
            String sessionId = (String) claims.get(JwtService.CLAIM_SESSION_ID);
            if (sessionId != null) {
                sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
                    s.revoke(now);
                    sessionRepository.save(s);
                });
            }
        } catch (Exception e) {
            // Safe ignore on logout invalidation
            log.debug("Logout token already invalid: {}", e.getMessage());
        }
    }
}
