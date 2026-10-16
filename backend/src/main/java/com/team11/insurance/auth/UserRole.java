package com.team11.insurance.auth;

/**
 * Standard user roles defined in the SAD v1.1 §4.3 and API Contract 02.
 * <ul>
 *   <li>CUSTOMER - End-user policyholder</li>
 *   <li>INSURER_ADMIN - Administrative staff for a specific insurer</li>
 *   <li>UNDERWRITER - Underwriting staff evaluating policy applications for an insurer</li>
 *   <li>CLAIMS_OFFICER - Claims processing staff for an insurer</li>
 *   <li>SYSTEM_ADMIN - Global platform administrator</li>
 * </ul>
 */
public enum UserRole {
    CUSTOMER("CUSTOMER"),
    INSURER_ADMIN("INSURER_ADMIN"),
    UNDERWRITER("UNDERWRITER"),
    CLAIMS_OFFICER("CLAIMS_OFFICER"),
    SYSTEM_ADMIN("SYSTEM_ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isStaff() {
        return this == INSURER_ADMIN || this == UNDERWRITER || this == CLAIMS_OFFICER;
    }

    public static UserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Role string cannot be null or empty");
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(roleStr.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + roleStr);
    }
}
