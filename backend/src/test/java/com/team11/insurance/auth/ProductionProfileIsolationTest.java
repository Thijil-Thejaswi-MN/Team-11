package com.team11.insurance.auth;

import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-02 Production Security and Profile Isolation Tests")
class ProductionProfileIsolationTest {

    @Test
    @DisplayName("CRITICAL: TestCurrentUserProvider MUST fail startup if 'prod' profile is active")
    void testTestProviderFailsInProduction() {
        MockEnvironment envProd = new MockEnvironment();
        envProd.setActiveProfiles("prod");

        TestCurrentUserProvider provider = new TestCurrentUserProvider(envProd);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::validateProfileIsolation);
        assertTrue(ex.getMessage().contains("TestCurrentUserProvider cannot be enabled in production profile"));
    }

    @Test
    @DisplayName("CRITICAL: TestCurrentUserProvider MUST fail startup if 'production' profile is active")
    void testTestProviderFailsInProductionAlias() {
        MockEnvironment envProd = new MockEnvironment();
        envProd.setActiveProfiles("production");

        TestCurrentUserProvider provider = new TestCurrentUserProvider(envProd);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::validateProfileIsolation);
        assertTrue(ex.getMessage().contains("TestCurrentUserProvider cannot be enabled in production profile"));
    }

    @Test
    @DisplayName("Production provider rejects unauthenticated access with AUTH-401")
    void testProductionProviderRejectsUnauthenticated() {
        ProductionCurrentUserProvider prodProvider = new ProductionCurrentUserProvider();
        RequestContextHolder.resetRequestAttributes();

        AuthenticationException ex = assertThrows(AuthenticationException.class, prodProvider::getCurrentUser);
        assertEquals(ErrorCode.AUTH_401, ex.getErrorCode());
        assertFalse(prodProvider.isAuthenticated());
    }

    @Test
    @DisplayName("Production provider ignores spoofed client-supplied request headers")
    void testProductionProviderIgnoresSpoofedHeaders() {
        ProductionCurrentUserProvider prodProvider = new ProductionCurrentUserProvider();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Attacker attempts to pass spoofed headers
        request.addHeader("X-Role", "SYSTEM_ADMIN");
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-Insurer-Id", "99");
        request.setParameter("role", "SYSTEM_ADMIN");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            // Must still reject because identity is only resolved from validated server-side session attribute
            AuthenticationException ex = assertThrows(AuthenticationException.class, prodProvider::getCurrentUser);
            assertEquals(ErrorCode.AUTH_401, ex.getErrorCode());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("Production provider accepts valid server-verified CurrentUser attribute")
    void testProductionProviderAcceptsVerifiedSession() {
        ProductionCurrentUserProvider prodProvider = new ProductionCurrentUserProvider();
        MockHttpServletRequest request = new MockHttpServletRequest();

        CurrentUser verifiedUser = new CurrentUser(1001L, UserRole.CUSTOMER, null);
        request.setAttribute(ProductionCurrentUserProvider.CURRENT_USER_ATTRIBUTE, verifiedUser);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            CurrentUser resolved = prodProvider.getCurrentUser();
            assertEquals(verifiedUser, resolved);
            assertTrue(prodProvider.isAuthenticated());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
