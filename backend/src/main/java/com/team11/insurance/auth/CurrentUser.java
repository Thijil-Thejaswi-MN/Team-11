package com.team11.insurance.auth;

import java.io.Serializable;
import java.util.Objects;

/**
 * Server-side identity context representing the currently authenticated user.
 * <p>
 * Defined in API Contract 02 (§6):
 * <pre>
 * {
 *   "userId": 1001,
 *   "role": "CUSTOMER",
 *   "insurerId": null
 * }
 * </pre>
 * <b>Security Note:</b> This context is created server-side by authentication and authorization
 * components. It MUST NEVER be constructed from client-supplied HTTP request headers or query
 * parameters (SAD §4.3.1, INS-SR-003).
 */
public final class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String role;
    private final Long insurerId;

    public CurrentUser(Long userId, String role, Long insurerId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be a positive non-null value");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("role must not be null or empty");
        }
        this.userId = userId;
        this.role = role.trim().toUpperCase();
        this.insurerId = insurerId;

        // Staff roles (INSURER_ADMIN, UNDERWRITER, CLAIMS_OFFICER) must belong to an insurer
        UserRole parsedRole = getUserRole();
        if (parsedRole != null && parsedRole.isStaff() && insurerId == null) {
            throw new IllegalArgumentException("Staff role '" + this.role + "' must be associated with a valid insurerId");
        }
    }

    public CurrentUser(Long userId, UserRole userRole, Long insurerId) {
        this(userId, userRole != null ? userRole.getValue() : null, insurerId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public UserRole getUserRole() {
        try {
            return UserRole.fromString(role);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isCustomer() {
        return UserRole.CUSTOMER.getValue().equalsIgnoreCase(role);
    }

    public boolean isInsurerAdmin() {
        return UserRole.INSURER_ADMIN.getValue().equalsIgnoreCase(role);
    }

    public boolean isUnderwriter() {
        return UserRole.UNDERWRITER.getValue().equalsIgnoreCase(role);
    }

    public boolean isClaimsOfficer() {
        return UserRole.CLAIMS_OFFICER.getValue().equalsIgnoreCase(role);
    }

    public boolean isSystemAdmin() {
        return UserRole.SYSTEM_ADMIN.getValue().equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        UserRole r = getUserRole();
        return r != null && r.isStaff();
    }

    public boolean hasRole(UserRole expectedRole) {
        return expectedRole != null && expectedRole.getValue().equalsIgnoreCase(this.role);
    }

    public boolean hasRole(String expectedRole) {
        return expectedRole != null && expectedRole.trim().equalsIgnoreCase(this.role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentUser that = (CurrentUser) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(role, that.role) &&
                Objects.equals(insurerId, that.insurerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role, insurerId);
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "userId=" + userId +
                ", role='" + role + '\'' +
                ", insurerId=" + insurerId +
                '}';
    }
}
