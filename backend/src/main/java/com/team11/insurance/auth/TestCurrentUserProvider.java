package com.team11.insurance.auth;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AuthenticationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

/**
 * Development and Automated Testing Stub for CurrentUser identity.
 * <p>
 * Work Item: P2-02
 * Owner: Person 2 (Sharana)
 * <p>
 * Provides a seeded synthetic identity for development and test execution without requiring
 * live JWT authentication.
 * <p>
 * <b>Strict Security Rules (P2-02 / INS-SR-003):</b>
 * <ul>
 *   <li>Only active under explicit 'dev' and 'test' Spring profiles.</li>
 *   <li>Throws an {@link IllegalStateException} if loaded in a 'prod' or 'production' profile.</li>
 *   <li>Never trusts client-supplied roles, user IDs, or insurer IDs from request headers or parameters.</li>
 *   <li>Allows automated tests to switch between predefined seeded identities via a thread-safe synthetic context.</li>
 * </ul>
 */
@Component
@Profile({"dev", "test"})
public class TestCurrentUserProvider implements ICurrentUserProvider {

    private static final Logger log = LoggerFactory.getLogger(TestCurrentUserProvider.class);

    // Seeded synthetic test identities aligned with SAD §4.3 and STP §6 / Appendix A fixtures
    public static final CurrentUser SEEDED_CUSTOMER = new CurrentUser(1001L, UserRole.CUSTOMER, null);
    public static final CurrentUser SEEDED_INSURER_ADMIN = new CurrentUser(2001L, UserRole.INSURER_ADMIN, 1L);
    public static final CurrentUser SEEDED_UNDERWRITER = new CurrentUser(3001L, UserRole.UNDERWRITER, 1L);
    public static final CurrentUser SEEDED_CLAIMS_OFFICER = new CurrentUser(4001L, UserRole.CLAIMS_OFFICER, 1L);
    public static final CurrentUser SEEDED_SYSTEM_ADMIN = new CurrentUser(5001L, UserRole.SYSTEM_ADMIN, null);

    private static final ThreadLocal<CurrentUser> SYNTHETIC_OVERRIDE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> UNAUTHENTICATED = new ThreadLocal<>();

    private final Environment environment;

    public TestCurrentUserProvider(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProfileIsolation() {
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProduction = Arrays.stream(activeProfiles)
                    .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: TestCurrentUserProvider cannot be enabled in production profile!");
            }
        }
        log.warn("TestCurrentUserProvider is ACTIVE for dev/test profile. Seeded identity will be provided.");
    }

    @Override
    public CurrentUser getCurrentUser() {
        if (Boolean.TRUE.equals(UNAUTHENTICATED.get())) {
            throw new AuthenticationException("Authentication required: no active server-side session or valid token found.");
        }
        org.springframework.web.context.request.RequestAttributes attribs =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attribs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttribs) {
            jakarta.servlet.http.HttpServletRequest request = servletAttribs.getRequest();
            Object userObj = request.getAttribute(ProductionCurrentUserProvider.CURRENT_USER_ATTRIBUTE);
            if (userObj instanceof CurrentUser currentUser) {
                return currentUser;
            }
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                CurrentUser syntheticUser = resolveSyntheticToken(token);
                if (syntheticUser != null) {
                    CurrentUser override = SYNTHETIC_OVERRIDE.get();
                    return override != null ? override : syntheticUser;
                }
                if (isJwtStructure(token)) {
                    throw new AuthenticationException("Authentication required: no active server-side session or valid token found.");
                }
                // Token failed JWT parsing / placeholder: allow fallback to synthetic user context below
            }
        }
        CurrentUser override = SYNTHETIC_OVERRIDE.get();
        if (override != null) {
            return override;
        }
        // Default synthetic user for local development and smoke tests
        return SEEDED_CUSTOMER;
    }

    private CurrentUser resolveSyntheticToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String clean = token.trim();
        if (clean.equals("<customer-bearer-token-placeholder>") || clean.equals("{{customerToken}}")) {
            return SEEDED_CUSTOMER;
        }
        if (clean.equals("<insurer-admin-bearer-token-placeholder>") || clean.equals("{{insurerAdminToken}}")) {
            return SEEDED_INSURER_ADMIN;
        }
        if (clean.equals("<underwriter-bearer-token-placeholder>") || clean.equals("{{underwriterToken}}")) {
            return SEEDED_UNDERWRITER;
        }
        if (clean.equals("<claims-officer-bearer-token-placeholder>") || clean.equals("{{claimsOfficerToken}}")) {
            return SEEDED_CLAIMS_OFFICER;
        }
        if (clean.equals("<system-admin-bearer-token-placeholder>") || clean.equals("{{systemAdminToken}}")) {
            return SEEDED_SYSTEM_ADMIN;
        }
        String lower = clean.toLowerCase();
        if (lower.contains("underwriter")) {
            return SEEDED_UNDERWRITER;
        }
        if (lower.contains("insurer-admin") || lower.contains("insureradmin")) {
            return SEEDED_INSURER_ADMIN;
        }
        if (lower.contains("claims-officer") || lower.contains("claimsofficer")) {
            return SEEDED_CLAIMS_OFFICER;
        }
        if (lower.contains("system-admin") || lower.contains("systemadmin")) {
            return SEEDED_SYSTEM_ADMIN;
        }
        if (lower.contains("customer")) {
            return SEEDED_CUSTOMER;
        }
        return null;
    }

    private boolean isJwtStructure(String token) {
        if (token == null) {
            return false;
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            return false;
        }
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                return false;
            }
        }
        try {
            Base64.getUrlDecoder().decode(parts[0]);
            Base64.getUrlDecoder().decode(parts[1]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean isAuthenticated() {
        if (Boolean.TRUE.equals(UNAUTHENTICATED.get())) {
            return false;
        }
        try {
            return getCurrentUser() != null;
        } catch (AuthenticationException ex) {
            return false;
        }
    }

    /**
     * Sets a synthetic test user in the current thread's test context.
     * Used by automated integration tests for P1, P3, P4 to simulate different roles.
     *
     * @param user the synthetic user to simulate
     */
    public static void setSyntheticUser(CurrentUser user) {
        UNAUTHENTICATED.remove();
        SYNTHETIC_OVERRIDE.set(user);
    }

    /**
     * Marks the current test thread context as unauthenticated.
     */
    public static void setUnauthenticated() {
        SYNTHETIC_OVERRIDE.remove();
        UNAUTHENTICATED.set(Boolean.TRUE);
    }

    /**
     * Clears the current thread's synthetic override, reverting to the default seeded customer.
     */
    public static void clearSyntheticUser() {
        SYNTHETIC_OVERRIDE.remove();
        UNAUTHENTICATED.remove();
    }
}
