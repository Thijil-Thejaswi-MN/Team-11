package com.team11.insurance.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-02 TestCurrentUserProvider Tests")
class TestCurrentUserProviderTest {

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
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
}
