package com.team11.insurance.auth.dto;

/**
 * Registration response DTO (API Contract §2.1).
 */
public class RegisterResponse {

    private Long userId;

    public RegisterResponse() {
    }

    public RegisterResponse(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
