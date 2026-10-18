package com.team11.insurance.auth;

import com.team11.insurance.auth.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P2-05 INS-SR-002: PasswordService and BCrypt Work Factor Tests")
class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    @DisplayName("BCrypt work factor is at least 12 per project security specification")
    void testBcryptWorkFactorIsTwelveOrHigher() {
        assertTrue(PasswordService.BCRYPT_WORK_FACTOR >= 12, "Work factor must be at least 12");
    }

    @Test
    @DisplayName("Password hash differs from plaintext password (never plaintext)")
    void testPasswordHashDiffersFromPlaintext() {
        String rawPassword = "SecurePassword123!";
        String hash = passwordService.hashPassword(rawPassword);

        assertNotNull(hash);
        assertNotEquals(rawPassword, hash);
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"),
                "BCrypt hash should start with $2a$12$ or $2b$12$");
    }

    @Test
    @DisplayName("Salting: same plaintext hashed twice generates distinct hashes")
    void testSaltedHashingProducesUniqueHashes() {
        String rawPassword = "SamePassword456$";
        String hash1 = passwordService.hashPassword(rawPassword);
        String hash2 = passwordService.hashPassword(rawPassword);

        assertNotEquals(hash1, hash2, "BCrypt must salt each hash independently");
        assertTrue(passwordService.matches(rawPassword, hash1));
        assertTrue(passwordService.matches(rawPassword, hash2));
    }

    @Test
    @DisplayName("Password verification succeeds for correct password and fails for incorrect")
    void testPasswordMatching() {
        String rawPassword = "CorrectPassword789#";
        String hash = passwordService.hashPassword(rawPassword);

        assertTrue(passwordService.matches(rawPassword, hash));
        assertFalse(passwordService.matches("WrongPassword789#", hash));
        assertFalse(passwordService.matches("", hash));
        assertFalse(passwordService.matches(null, hash));
    }

    @Test
    @DisplayName("Reject null or blank password hashing")
    void testRejectNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> passwordService.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> passwordService.hashPassword("   "));
    }
}
