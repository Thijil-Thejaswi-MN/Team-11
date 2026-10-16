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
        CurrentUser override = SYNTHETIC_OVERRIDE.get();
        if (override != null) {
            return override;
        }
        // Default synthetic user for local development and smoke tests
        return SEEDED_CUSTOMER;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    /**
     * Sets a synthetic test user in the current thread's test context.
     * Used by automated integration tests for P1, P3, P4 to simulate different roles.
     *
     * @param user the synthetic user to simulate
     */
    public static void setSyntheticUser(CurrentUser user) {
        SYNTHETIC_OVERRIDE.set(user);
    }

    /**
     * Clears the current thread's synthetic override, reverting to the default seeded customer.
     */
    public static void clearSyntheticUser() {
        SYNTHETIC_OVERRIDE.remove();
    }
}
