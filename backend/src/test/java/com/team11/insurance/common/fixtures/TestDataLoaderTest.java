package com.team11.insurance.common.fixtures;

import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("P4-05 Canonical TestDataLoader Integration Tests")
class TestDataLoaderTest {

    @Autowired
    private TestDataLoader testDataLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InsurerRepository insurerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    @Transactional
    @DisplayName("Loads canonical test fixtures in strict FK dependency order")
    void testCanonicalFixturesLoadedSuccessfully() {
        testDataLoader.loadCanonicalFixtures(true);

        // 1. Insurers (insurers.json has 2 insurers)
        assertEquals(2, insurerRepository.count(), "Insurers count should match canonical fixture");
        assertTrue(insurerRepository.findByNameIgnoreCase("SafeShield General").isPresent());
        assertTrue(insurerRepository.findByNameIgnoreCase("Bharat Care Insurance").isPresent());

        // 2. Users (users.json has 10 users: 3 customers, 6 insurer staff, 1 sysadmin)
        assertEquals(10, userRepository.count(), "Users count should match canonical fixture");
        assertTrue(userRepository.findByEmailIgnoreCase("asha@example.test").isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase("sysadmin@insurance.test").isPresent());

        // 3. Plans (plans.json has 7 plans)
        assertEquals(7, planRepository.count(), "Plans count should match canonical fixture");
        assertTrue(planRepository.findByCode("PLAN-A-M1").isPresent());
        assertTrue(planRepository.findByCode("PLAN-B-H1").isPresent());

        // 4. Policies (policies.json has 3 policies)
        assertEquals(3, policyRepository.count(), "Policies count should match canonical fixture");
        List<Policy> policies = policyRepository.findAll();
        assertTrue(policies.stream().anyMatch(p -> "POL-H-1".equals(p.getPolicyNo()) && p.getStatus() == PolicyStatus.ACTIVE));
        assertTrue(policies.stream().anyMatch(p -> "POL-M-1".equals(p.getPolicyNo()) && p.getStatus() == PolicyStatus.ACTIVE));
    }

    @Test
    @Transactional
    @DisplayName("Idempotent / repeatable loading does not duplicate fixture records")
    void testRepeatableLoadingIsIdempotent() {
        testDataLoader.loadCanonicalFixtures(false);
        long initialInsurers = insurerRepository.count();
        long initialUsers = userRepository.count();
        long initialPlans = planRepository.count();
        long initialPolicies = policyRepository.count();

        // Second run without reset must preserve existing counts
        testDataLoader.loadCanonicalFixtures(false);
        assertEquals(initialInsurers, insurerRepository.count());
        assertEquals(initialUsers, userRepository.count());
        assertEquals(initialPlans, planRepository.count());
        assertEquals(initialPolicies, policyRepository.count());
    }

    @Test
    @DisplayName("Deliberate fixture load failure rolls back transaction atomically and propagates exception")
    void testFixtureLoadFailureRollsBackTransaction() {
        // Clear all fixture data
        jdbcTemplate.execute("DELETE FROM policies");
        jdbcTemplate.execute("DELETE FROM plans");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM insurers");
        assertEquals(0, insurerRepository.count());
        assertEquals(0, userRepository.count());

        // Add a check constraint on users that cannot be satisfied by users.json (which has email strings)
        // This allows loadInsurers() to succeed, but causes loadUsers() to fail
        jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT chk_deliberate_fail CHECK (email = 'IMPOSSIBLE_EMAIL')");

        try {
            // Attempt loading fixtures - must throw exception
            assertThrows(Exception.class, () -> testDataLoader.loadCanonicalFixtures(false));

            // Verify transaction rolled back: insurers inserted before the failure must have been rolled back
            assertEquals(0, insurerRepository.count(), "Insurers must be rolled back after users fixture failure");
            assertEquals(0, userRepository.count(), "Users must be rolled back");
        } finally {
            // Clean up the constraint so other tests are not affected
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT chk_deliberate_fail");
        }

        // Verify recovery: subsequent fixture loading succeeds cleanly
        assertDoesNotThrow(() -> testDataLoader.loadCanonicalFixtures(true));
        assertEquals(2, insurerRepository.count());
        assertEquals(10, userRepository.count());
    }
}
