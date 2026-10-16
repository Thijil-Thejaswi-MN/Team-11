package com.team11.insurance.auth.dto;

/**
 * User profile response DTO for /api/v1/auth/me.
 */
public class UserProfileResponse {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Long insurerId;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long userId, String name, String email, String phone, String role, Long insurerId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.insurerId = insurerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }
}
