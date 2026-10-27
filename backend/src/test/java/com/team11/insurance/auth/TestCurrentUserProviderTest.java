package com.team11.insurance.auth;

import com.team11.insurance.common.exception.AuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-02 TestCurrentUserProvider Tests")
class TestCurrentUserProviderTest {

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Default seeded synthetic identity is Customer Asha Verma (1001)")
    void testDefaultSeededUser() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);
        provider.validateProfileIsolation();

        CurrentUser user = provider.getCurrentUser();
        assertNotNull(user);
        assertEquals(1001L, user.getUserId());
        assertEquals("CUSTOMER", user.getRole());
        assertNull(user.getInsurerId());
        assertTrue(provider.isAuthenticated());
    }

    @Test
    @DisplayName("Thread-safe synthetic override allows testing staff and admin roles")
    void testSyntheticOverrides() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);

        // Switch to Insurer Admin
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN);
        CurrentUser admin = provider.getCurrentUser();
        assertEquals(2001L, admin.getUserId());
        assertEquals("INSURER_ADMIN", admin.getRole());
        assertEquals(1L, admin.getInsurerId());

        // Switch to Underwriter
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);
        CurrentUser uw = provider.getCurrentUser();
        assertEquals(3001L, uw.getUserId());
        assertEquals("UNDERWRITER", uw.getRole());
        assertEquals(1L, uw.getInsurerId());

        // Clear reverts back to default seeded customer
        TestCurrentUserProvider.clearSyntheticUser();
        CurrentUser reverted = provider.getCurrentUser();
        assertEquals(1001L, reverted.getUserId());
        assertEquals("CUSTOMER", reverted.getRole());
    }

    @Test
    @DisplayName("Predefined fixtures match STP §6 and SAD §4.3 specifications")
    void testPredefinedFixtures() {
        assertEquals(1001L, TestCurrentUserProvider.SEEDED_CUSTOMER.getUserId());
        assertNull(TestCurrentUserProvider.SEEDED_CUSTOMER.getInsurerId());

        assertEquals(2001L, TestCurrentUserProvider.SEEDED_INSURER_ADMIN.getUserId());
        assertEquals(1L, TestCurrentUserProvider.SEEDED_INSURER_ADMIN.getInsurerId());

        assertEquals(3001L, TestCurrentUserProvider.SEEDED_UNDERWRITER.getUserId());
        assertEquals(1L, TestCurrentUserProvider.SEEDED_UNDERWRITER.getInsurerId());

        assertEquals(4001L, TestCurrentUserProvider.SEEDED_CLAIMS_OFFICER.getUserId());
        assertEquals(1L, TestCurrentUserProvider.SEEDED_CLAIMS_OFFICER.getInsurerId());

        assertEquals(5001L, TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN.getUserId());
        assertNull(TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN.getInsurerId());
    }

    @Test
    @DisplayName("Placeholder tokens from Postman/Newman environment resolve to appropriate seeded fixtures")
    void testPlaceholderTokensResolveToSeededFixtures() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);

        // Customer placeholder
        MockHttpServletRequest reqCustomer = new MockHttpServletRequest();
        reqCustomer.addHeader("Authorization", "Bearer <customer-bearer-token-placeholder>");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(reqCustomer));
        CurrentUser cust = provider.getCurrentUser();
        assertEquals(1001L, cust.getUserId());
        assertEquals("CUSTOMER", cust.getRole());

        // Underwriter placeholder
        MockHttpServletRequest reqUw = new MockHttpServletRequest();
        reqUw.addHeader("Authorization", "Bearer <underwriter-bearer-token-placeholder>");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(reqUw));
        CurrentUser uw = provider.getCurrentUser();
        assertEquals(3001L, uw.getUserId());
        assertEquals("UNDERWRITER", uw.getRole());
        assertEquals(1L, uw.getInsurerId());

        // Insurer Admin placeholder
        MockHttpServletRequest reqAdmin = new MockHttpServletRequest();
        reqAdmin.addHeader("Authorization", "Bearer {{insurerAdminToken}}");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(reqAdmin));
        CurrentUser admin = provider.getCurrentUser();
        assertEquals(2001L, admin.getUserId());
        assertEquals("INSURER_ADMIN", admin.getRole());

        // Claims Officer placeholder
        MockHttpServletRequest reqClaims = new MockHttpServletRequest();
        reqClaims.addHeader("Authorization", "Bearer <claims-officer-bearer-token-placeholder>");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(reqClaims));
        CurrentUser claims = provider.getCurrentUser();
        assertEquals(4001L, claims.getUserId());
        assertEquals("CLAIMS_OFFICER", claims.getRole());

        // System Admin placeholder
        MockHttpServletRequest reqSys = new MockHttpServletRequest();
        reqSys.addHeader("Authorization", "Bearer <system-admin-bearer-token-placeholder>");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(reqSys));
        CurrentUser sys = provider.getCurrentUser();
        assertEquals(5001L, sys.getUserId());
        assertEquals("SYSTEM_ADMIN", sys.getRole());
    }

    @Test
    @DisplayName("Non-JWT malformed placeholder tokens fall back to synthetic context without throwing")
    void testMalformedTokensAllowFallback() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer mock-token-without-jwt-structure");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        CurrentUser user = provider.getCurrentUser();
        assertNotNull(user);
        assertEquals(1001L, user.getUserId());
        assertEquals("CUSTOMER", user.getRole());
    }

    @Test
    @DisplayName("Structurally valid JWT that lacks authenticated session throws AuthenticationException")
    void testStructurallyValidJwtWithoutSessionThrows() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);

        // Header: {"alg":"HS256"}, Payload: {"sub":"1001"}
        String validHeader = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String validPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1001\"}".getBytes());
        String pseudoJwt = validHeader + "." + validPayload + ".pseudoSignaturePart";

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + pseudoJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        assertThrows(AuthenticationException.class, provider::getCurrentUser);
    }

    @Test
    @DisplayName("Explicit unauthenticated state throws AuthenticationException even with synthetic tokens")
    void testUnauthenticatedThrowsEvenWithPlaceholder() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        TestCurrentUserProvider provider = new TestCurrentUserProvider(env);

        TestCurrentUserProvider.setUnauthenticated();

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer <customer-bearer-token-placeholder>");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        assertThrows(AuthenticationException.class, provider::getCurrentUser);
    }
}
