package com.team11.insurance.auth;

import com.team11.insurance.auth.annotation.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("P2-02 CurrentUserArgumentResolver MVC Integration Tests")
class CurrentUserArgumentResolverTest {

    private MockMvc mockMvc;
    private TestCurrentUserProvider testProvider;

    @RestController
    static class TestIdentityController {

        @GetMapping("/api/v1/test/current-user")
        public ResponseEntity<Map<String, Object>> getCurrentUserDirect(com.team11.insurance.auth.CurrentUser user) {
            return ResponseEntity.ok(Map.of(
                    "userId", user.getUserId(),
                    "role", user.getRole(),
                    "insurerId", user.getInsurerId() != null ? user.getInsurerId() : -1
            ));
        }

        @GetMapping("/api/v1/test/annotated-user")
        public ResponseEntity<Map<String, Object>> getCurrentUserAnnotated(@CurrentUser com.team11.insurance.auth.CurrentUser user) {
            return ResponseEntity.ok(Map.of(
                    "userId", user.getUserId(),
                    "role", user.getRole(),
                    "insurerId", user.getInsurerId() != null ? user.getInsurerId() : -1
            ));
        }
    }

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        testProvider = new TestCurrentUserProvider(env);
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver(testProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestIdentityController())
                .setCustomArgumentResolvers(resolver)
                .build();
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("Resolves default seeded customer into controller parameter")
    void testResolveDefaultUser() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.insurerId").value(-1));
    }

    @Test
    @DisplayName("Resolves parameter annotated with @CurrentUser")
    void testResolveAnnotatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/test/annotated-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Spoofed headers are ignored; attacker cannot elevate to SYSTEM_ADMIN")
    void testSpoofedHeadersAreIgnored() throws Exception {
        mockMvc.perform(get("/api/v1/test/current-user")
                        .header("X-Role", "SYSTEM_ADMIN")
                        .header("X-User-Id", "9999")
                        .header("X-Insurer-Id", "1")
                        .param("role", "SYSTEM_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Simulate Underwriter test identity in test suite")
    void testSimulatedUnderwriterIdentity() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        mockMvc.perform(get("/api/v1/test/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3001))
                .andExpect(jsonPath("$.role").value("UNDERWRITER"))
                .andExpect(jsonPath("$.insurerId").value(1));
    }
}
