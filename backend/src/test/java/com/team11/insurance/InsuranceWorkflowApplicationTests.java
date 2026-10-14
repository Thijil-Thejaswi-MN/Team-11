package com.team11.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring Boot ApplicationContext initializes cleanly.
 * Uses isolated in-memory test profile with H2 (no live MySQL credentials required).
 */
@SpringBootTest
@ActiveProfiles("test")
class InsuranceWorkflowApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test: verifies beans, modular packages, and configurations wire up cleanly
    }
}
