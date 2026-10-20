package com.team11.insurance.admin.dto;

import com.team11.insurance.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * System Admin request DTO for creating users/staff (SAD §4.3.6, API Contract §5.1).
 */
public class CreateUserRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid format")
    private String email;

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters long")
    private String password;

    @NotNull(message = "role is required")
    private UserRole role;

    /**
     * Required for staff roles (INSURER_ADMIN, UNDERWRITER, CLAIMS_OFFICER).
     */
    private Long insurerId;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String name, String email, String phone, String password, UserRole role, Long insurerId) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.insurerId = insurerId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }
}
