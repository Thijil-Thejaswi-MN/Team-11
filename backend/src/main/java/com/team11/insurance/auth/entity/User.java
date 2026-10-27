package com.team11.insurance.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team11.insurance.auth.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Persistent user entity representing all system actors (INS-F-001, INS-SR-002, INS-SR-003).
 * <p>
 * Passwords are strictly stored as BCrypt salted hashes (work factor >= 12).
 * Plaintext passwords MUST NEVER be stored, serialized, or logged.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_insurer", columnList = "insurer_id")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    /**
     * Salted BCrypt password hash. Never serialize to JSON or expose in API responses.
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * Associated insurer ID for staff roles (INSURER_ADMIN, UNDERWRITER, CLAIMS_OFFICER).
     * Null for CUSTOMER and SYSTEM_ADMIN.
     */
    @Column(name = "insurer_id")
    private Long insurerId;

    /**
     * Consecutive failed login attempt counter for account lockout (INS-SR-005, SAD §4.3.1).
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /**
     * Account lockout expiration timestamp. If set and in the future, login is denied with AUTH-423.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
    }

    public User(String name, String email, String phone, String passwordHash, UserRole role, Long insurerId) {
        this.name = name;
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.CUSTOMER;
        this.insurerId = insurerId;
        this.failedLoginAttempts = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isAccountLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public void recordFailedLogin(Instant now, int maxAttempts, int lockoutMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plusSeconds((long) lockoutMinutes * 60);
        }
    }

    public void resetLoginLockout() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + com.team11.insurance.common.logging.SensitiveDataMasker.maskEmail(email) + '\'' +
                ", phone='" + com.team11.insurance.common.logging.SensitiveDataMasker.maskPhone(phone) + '\'' +
                ", role=" + role +
                ", insurerId=" + insurerId +
                '}';
    }
}
