package com.team11.insurance.auth.dto;

/**
 * Login response DTO (API Contract §2.2).
 */
public class LoginResponse {

    private String accessToken;
    private String role;
    private int expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String role, int expiresIn) {
        this.accessToken = accessToken;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
}
