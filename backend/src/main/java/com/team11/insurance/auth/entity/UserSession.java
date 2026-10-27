package com.team11.insurance.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Server-side session tracking entity (INS-SR-005, SAD §4.3.1).
 * <p>
 * Enforces a strict 15-minute inactivity timeout. If a session is idle for more than
 * 15 minutes, it is invalidated and denied with AUTH-401.
 */
@Entity
@Table(name = "user_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_session_id", columnNames = "session_id")
        },
        indexes = {
                @Index(name = "idx_session_id", columnList = "session_id"),
                @Index(name = "idx_session_user", columnList = "user_id"),
                @Index(name = "idx_session_active", columnList = "revoked, expires_at")
        })
public class UserSession {

    public static final int INACTIVITY_TIMEOUT_SECONDS = 900; // 15 minutes per SAD §4.3.1
    public static final int MAX_SESSION_LIFETIME_SECONDS = 28800; // 8 hours absolute maximum per TC-Sec-10

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64, unique = true)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UserSession() {
    }

    public UserSession(String sessionId, Long userId, Instant now, int validitySeconds, String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = now;
        this.lastAccessedAt = now;
        this.expiresAt = now.plusSeconds(validitySeconds > 0 ? validitySeconds : MAX_SESSION_LIFETIME_SECONDS);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.revoked = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isValid(Instant now) {
        if (revoked) {
            return false;
        }
        if (now.isAfter(expiresAt)) {
            return false;
        }
        // Check inactivity timeout (15 minutes)
        return !now.isAfter(lastAccessedAt.plusSeconds(INACTIVITY_TIMEOUT_SECONDS));
    }

    public void touch(Instant now) {
        this.lastAccessedAt = now;
    }

    public void revoke(Instant now) {
        this.revoked = true;
        this.revokedAt = now;
    }
}
