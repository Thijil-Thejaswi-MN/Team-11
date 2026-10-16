package com.team11.insurance.auth;

import com.team11.insurance.common.exception.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-02 Role and Insurer Isolation Security Tests (INS-SR-003)")
class RoleSecurityTest {

    @Test
    @DisplayName("assertRole allows authorized roles and throws ACCESS-403 for unauthorized")
    void testAssertRole() {
        CurrentUser customer = new CurrentUser(1001L, UserRole.CUSTOMER, null);
        CurrentUser underwriter = new CurrentUser(3001L, UserRole.UNDERWRITER, 1L);

        // Customer allowed
        assertDoesNotThrow(() -> RoleSecurity.assertRole(customer, UserRole.CUSTOMER));

        // Customer denied on Underwriter operation
        AccessDeniedException ex1 = assertThrows(AccessDeniedException.class, () ->
                RoleSecurity.assertRole(customer, UserRole.UNDERWRITER, UserRole.INSURER_ADMIN));
        assertTrue(ex1.getMessage().contains("is not authorized"));

        // Underwriter allowed
        assertDoesNotThrow(() -> RoleSecurity.assertRole(underwriter, UserRole.UNDERWRITER));
    }

    @Test
    @DisplayName("assertInsurerAccess enforces insurer boundary for staff")
    void testInsurerIsolation() {
        CurrentUser underwriterInsurer1 = new CurrentUser(3001L, UserRole.UNDERWRITER, 1L);
        CurrentUser sysAdmin = new CurrentUser(5001L, UserRole.SYSTEM_ADMIN, null);
        CurrentUser customer = new CurrentUser(1001L, UserRole.CUSTOMER, null);

        // Same insurer allowed
        assertDoesNotThrow(() -> RoleSecurity.assertInsurerAccess(underwriterInsurer1, 1L));

        // Cross-insurer denied (ACCESS-403)
        AccessDeniedException ex1 = assertThrows(AccessDeniedException.class, () ->
                RoleSecurity.assertInsurerAccess(underwriterInsurer1, 2L));
        assertTrue(ex1.getMessage().contains("insurer isolation violation"));

        // System Admin can access any insurer
        assertDoesNotThrow(() -> RoleSecurity.assertInsurerAccess(sysAdmin, 1L));
        assertDoesNotThrow(() -> RoleSecurity.assertInsurerAccess(sysAdmin, 2L));

        // Customer cannot access insurer staff operations
        assertThrows(AccessDeniedException.class, () -> RoleSecurity.assertInsurerAccess(customer, 1L));
    }

    @Test
    @DisplayName("assertCustomerOrStaffAccess ensures customers cannot view other customer data")
    void testCustomerDataIsolation() {
        CurrentUser customer1 = new CurrentUser(1001L, UserRole.CUSTOMER, null);
        CurrentUser customer2 = new CurrentUser(1002L, UserRole.CUSTOMER, null);
        CurrentUser staff = new CurrentUser(3001L, UserRole.UNDERWRITER, 1L);

        // Customer viewing own data
        assertDoesNotThrow(() -> RoleSecurity.assertCustomerOrStaffAccess(customer1, 1001L));

        // Customer viewing another customer's data throws ACCESS-403
        assertThrows(AccessDeniedException.class, () ->
                RoleSecurity.assertCustomerOrStaffAccess(customer2, 1001L));

        // Staff viewing customer data allowed
        assertDoesNotThrow(() -> RoleSecurity.assertCustomerOrStaffAccess(staff, 1001L));
    }
}
