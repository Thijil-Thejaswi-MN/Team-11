package com.team11.insurance.auth;

import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.entity.UserSession;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.repository.UserSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("P2-05 Persistence Tests: User, Insurer, and Session Constraints")
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Test
    @DisplayName("User persistence saves salted hash, role, and timestamps")
    void testSaveUser() {
        User user = new User("Jane Customer", "jane.customer@test.invalid", "9876543210",
                "$2a$12$abcdefghijklmnopqrstuv", UserRole.CUSTOMER, null);

        User saved = userRepository.saveAndFlush(user);

        assertNotNull(saved.getId());
        assertEquals("jane.customer@test.invalid", saved.getEmail());
        assertEquals(UserRole.CUSTOMER, saved.getRole());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(0, saved.getFailedLoginAttempts());
        assertNull(saved.getLockedUntil());
    }

    @Test
    @DisplayName("Email uniqueness constraint is enforced at persistence layer")
    void testUniqueEmailConstraint() {
        User user1 = new User("User One", "duplicate@test.invalid", "1111111111",
                "$2a$12$hash1", UserRole.CUSTOMER, null);
        userRepository.saveAndFlush(user1);

        User user2 = new User("User Two", "duplicate@test.invalid", "2222222222",
                "$2a$12$hash2", UserRole.CUSTOMER, null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("Insurer persistence enforces unique name constraint")
    void testUniqueInsurerNameConstraint() {
        Insurer ins1 = new Insurer("National General Insurance");
        insurerRepository.saveAndFlush(ins1);

        Insurer ins2 = new Insurer("National General Insurance");
        assertThrows(DataIntegrityViolationException.class, () -> {
            insurerRepository.saveAndFlush(ins2);
        });
    }

    @Test
    @DisplayName("UserSession tracks inactivity timeout and revocation")
    void testUserSessionLifecycle() {
        Instant now = Instant.now();
        UserSession session = new UserSession("sess-uuid-12345", 1001L, now, 900, "127.0.0.1", "JUnit-Agent");
        UserSession saved = sessionRepository.saveAndFlush(session);

        assertNotNull(saved.getId());
        assertEquals("sess-uuid-12345", saved.getSessionId());
        assertTrue(saved.isValid(now));

        // Touch updates lastAccessedAt
        Instant touchTime = now.plusSeconds(300);
        saved.touch(touchTime);
        assertEquals(touchTime, saved.getLastAccessedAt());

        // Inactivity timeout: after 901 seconds of inactivity, session is invalid
        Instant idleTimeoutTime = touchTime.plusSeconds(UserSession.INACTIVITY_TIMEOUT_SECONDS + 1);
        assertFalse(saved.isValid(idleTimeoutTime));

        // Revocation invalidates immediately
        saved.revoke(now);
        assertTrue(saved.isRevoked());
        assertFalse(saved.isValid(now));
    }
}
