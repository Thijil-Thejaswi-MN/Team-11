package com.team11.insurance.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for salted BCrypt password hashing and verification (INS-SR-002).
 * <p>
 * Enforces a work factor (strength) of 12 per project security specification.
 * Plaintext passwords are never logged, stored, or leaked in error responses.
 */
@Service
public class PasswordService {

    public static final int BCRYPT_WORK_FACTOR = 12;

    private final BCryptPasswordEncoder encoder;

    public PasswordService() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_WORK_FACTOR);
    }

    /**
     * Hashes raw password using BCrypt with work factor 12.
     *
     * @param rawPassword plaintext password
     * @return salted BCrypt hash
     */
    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * Verifies raw password against the stored BCrypt hash.
     *
     * @param rawPassword plaintext password
     * @param encodedHash stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public boolean matches(String rawPassword, String encodedHash) {
        if (rawPassword == null || encodedHash == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedHash);
    }

    /**
     * Generates a dummy hash used to equalize execution time during invalid-email login attempts
     * to prevent username enumeration via timing analysis.
     */
    public String getDummyHash() {
        // Pre-computed BCrypt hash (factor 12) for dummy comparison
        return "$2a$12$e80yqVb0vXW.N5j0pX3q1e4F2z8K1a3b5c7d9e1f3g5h7i9j1k3m5";
    }
}
