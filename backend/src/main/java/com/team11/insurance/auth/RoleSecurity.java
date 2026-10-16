package com.team11.insurance.auth;

import com.team11.insurance.common.exception.AccessDeniedException;

import java.util.Arrays;

/**
 * Authorization and tenant-isolation utility methods for backend controllers and services.
 * Enforces SAD §4.3 and §4.4 (ACCESS-403) and INS-SR-003 (Role-based access control and insurer isolation).
 */
public final class RoleSecurity {

    private RoleSecurity() {}

    /**
     * Asserts that the authenticated user possesses one of the allowed roles.
     *
     * @param user current authenticated user
     * @param allowedRoles roles permitted to access the resource
     * @throws AccessDeniedException if the user does not possess any of the allowed roles
     */
    public static void assertRole(CurrentUser user, UserRole... allowedRoles) {
        if (user == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        UserRole currentRole = user.getUserRole();
        if (currentRole == null) {
            throw new AccessDeniedException("Access denied: unknown role '" + user.getRole() + "'.");
        }
        boolean matched = Arrays.stream(allowedRoles).anyMatch(r -> r == currentRole);
        if (!matched) {
            throw new AccessDeniedException("Access denied: role '" + currentRole.getValue() + "' is not authorized for this operation.");
        }
    }

    /**
     * Enforces server-side insurer isolation (SAD §4.3.1, INS-SR-003).
     * Staff members (Insurer Admin, Underwriter, Claims Officer) can only access records
     * belonging to their own insurer. System Admin can access all insurers.
     *
     * @param user current authenticated user
     * @param resourceInsurerId the insurer ID associated with the target resource
     * @throws AccessDeniedException if staff user attempts cross-insurer access
     */
    public static void assertInsurerAccess(CurrentUser user, Long resourceInsurerId) {
        if (user == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (user.isSystemAdmin()) {
            return; // Global system admin has cross-insurer access
        }
        if (user.isStaff()) {
            if (user.getInsurerId() == null || !user.getInsurerId().equals(resourceInsurerId)) {
                throw new AccessDeniedException("Access denied: insurer isolation violation. Caller insurerId="
                        + user.getInsurerId() + " cannot access resource of insurerId=" + resourceInsurerId);
            }
            return;
        }
        // Customers are not permitted to access insurer staff operations
        throw new AccessDeniedException("Access denied: non-staff user cannot access insurer-scoped operation.");
    }

    /**
     * Asserts that a customer can only access their own records, or staff/admin can access them.
     *
     * @param user current authenticated user
     * @param targetCustomerId customer ID of the record owner
     * @throws AccessDeniedException if another customer attempts to access the record
     */
    public static void assertCustomerOrStaffAccess(CurrentUser user, Long targetCustomerId) {
        if (user == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (user.isStaff() || user.isSystemAdmin()) {
            return;
        }
        if (!user.getUserId().equals(targetCustomerId)) {
            throw new AccessDeniedException("Access denied: customers may only access their own records.");
        }
    }
}
