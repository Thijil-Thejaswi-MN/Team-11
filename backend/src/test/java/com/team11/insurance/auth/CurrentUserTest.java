package com.team11.insurance.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-02 CurrentUser Identity Contract Tests")
class CurrentUserTest {

    @Test
    @DisplayName("Successfully construct valid Customer identity")
    void testValidCustomer() {
        CurrentUser user = new CurrentUser(1001L, "CUSTOMER", null);
        assertEquals(1001L, user.getUserId());
        assertEquals("CUSTOMER", user.getRole());
        assertNull(user.getInsurerId());
        assertTrue(user.isCustomer());
        assertFalse(user.isStaff());
        assertFalse(user.isInsurerAdmin());
        assertTrue(user.hasRole(UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("Successfully construct valid Insurer Admin identity with insurerId")
    void testValidInsurerAdmin() {
        CurrentUser user = new CurrentUser(2001L, UserRole.INSURER_ADMIN, 1L);
        assertEquals(2001L, user.getUserId());
        assertEquals("INSURER_ADMIN", user.getRole());
        assertEquals(1L, user.getInsurerId());
        assertTrue(user.isInsurerAdmin());
        assertTrue(user.isStaff());
        assertFalse(user.isCustomer());
    }

    @Test
    @DisplayName("Successfully construct Underwriter and Claims Officer identities")
    void testUnderwriterAndClaimsOfficer() {
        CurrentUser uw = new CurrentUser(3001L, UserRole.UNDERWRITER, 2L);
        assertTrue(uw.isUnderwriter());
        assertTrue(uw.isStaff());
        assertEquals(2L, uw.getInsurerId());

        CurrentUser co = new CurrentUser(4001L, UserRole.CLAIMS_OFFICER, 2L);
        assertTrue(co.isClaimsOfficer());
        assertTrue(co.isStaff());
        assertEquals(2L, co.getInsurerId());
    }

    @Test
    @DisplayName("Successfully construct System Admin identity (global, no insurerId)")
    void testValidSystemAdmin() {
        CurrentUser admin = new CurrentUser(5001L, UserRole.SYSTEM_ADMIN, null);
        assertTrue(admin.isSystemAdmin());
        assertFalse(admin.isStaff());
        assertNull(admin.getInsurerId());
    }

    @Test
    @DisplayName("Staff roles without insurerId must be rejected with IllegalArgumentException")
    void testStaffWithoutInsurerRejected() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                new CurrentUser(2001L, "INSURER_ADMIN", null));
        assertTrue(ex1.getMessage().contains("must be associated with a valid insurerId"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                new CurrentUser(3001L, "UNDERWRITER", null));
        assertTrue(ex2.getMessage().contains("must be associated with a valid insurerId"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                new CurrentUser(4001L, "CLAIMS_OFFICER", null));
        assertTrue(ex3.getMessage().contains("must be associated with a valid insurerId"));
    }

    @Test
    @DisplayName("Invalid or null userId must be rejected")
    void testInvalidUserId() {
        assertThrows(IllegalArgumentException.class, () -> new CurrentUser(null, "CUSTOMER", null));
        assertThrows(IllegalArgumentException.class, () -> new CurrentUser(0L, "CUSTOMER", null));
        assertThrows(IllegalArgumentException.class, () -> new CurrentUser(-5L, "CUSTOMER", null));
    }

    @Test
    @DisplayName("Invalid or blank role must be rejected")
    void testInvalidRole() {
        assertThrows(IllegalArgumentException.class, () -> new CurrentUser(1001L, (String) null, null));
        assertThrows(IllegalArgumentException.class, () -> new CurrentUser(1001L, "   ", null));
    }

    @Test
    @DisplayName("Equality and hashCode contract verification")
    void testEquality() {
        CurrentUser user1 = new CurrentUser(1001L, "CUSTOMER", null);
        CurrentUser user2 = new CurrentUser(1001L, "CUSTOMER", null);
        CurrentUser user3 = new CurrentUser(2001L, "INSURER_ADMIN", 1L);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1, user3);
    }
}
