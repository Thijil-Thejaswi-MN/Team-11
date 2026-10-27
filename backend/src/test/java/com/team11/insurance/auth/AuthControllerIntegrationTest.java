package com.team11.insurance.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.dto.LoginRequest;
import com.team11.insurance.auth.dto.RegisterRequest;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.auth.repository.UserSessionRepository;
import com.team11.insurance.common.time.MutableTestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-05 AuthController REST API Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private MutableTestClock clock;

    @BeforeEach
    void cleanUp() {
        clock.reset();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    @Test
    @DisplayName("POST /auth/register creates user and returns 201 Created with userId")
    void testRegisterSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice Test", "alice@test.invalid", "9988776655", "StrongPassword123!");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    @DisplayName("POST /auth/register rejects duplicate email with 409 Conflict")
    void testRegisterDuplicateEmailReturns409() throws Exception {
        RegisterRequest req1 = new RegisterRequest("User One", "dupe@test.invalid", "1111111111", "StrongPassword123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        RegisterRequest req2 = new RegisterRequest("User Two", "dupe@test.invalid", "2222222222", "AnotherPassword123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE-409"));
    }

    @Test
    @DisplayName("POST /auth/register with invalid email and short password returns 400 Bad Request")
    void testRegisterInvalidInputReturns400() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest("", "not-an-email", "", "short");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }

    @Test
    @DisplayName("POST /auth/login with valid credentials returns 200 OK, token, role, and expiresIn")
    void testLoginSuccess() throws Exception {
        RegisterRequest regReq = new RegisterRequest("Bob Test", "bob@test.invalid", "8888888888", "ValidPassword123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("bob@test.invalid", "ValidPassword123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /auth/login with wrong password returns 401 Unauthorized")
    void testLoginWrongPasswordReturns401() throws Exception {
        RegisterRequest regReq = new RegisterRequest("Charlie Test", "charlie@test.invalid", "7777777777", "CorrectPassword123!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("charlie@test.invalid", "IncorrectPassword123!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }

    @Test
    @DisplayName("POST /auth/login: 5 consecutive failures triggers 423 Locked")
    void testLoginFiveFailuresTriggers423Locked() throws Exception {
        RegisterRequest regReq = new RegisterRequest("Lock Test", "lock@test.invalid", "6666666666", "Password12345!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = new LoginRequest("lock@test.invalid", "WrongPassword!");

        // First 4 attempts return 401
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 5th attempt triggers 423 Locked
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("AUTH-423"));

        // During 15-minute lockout, even correct password returns 423 Locked (TC-Sec-05)
        LoginRequest correctLogin = new LoginRequest("lock@test.invalid", "Password12345!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("AUTH-423"));

        // Advance clock by 15 minutes + 1 second
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        // After lockout duration expires, login with correct password succeeds (TC-Sec-05)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("GET /auth/me with Bearer token returns authenticated user profile")
    void testGetAuthMeWithToken() throws Exception {
        RegisterRequest regReq = new RegisterRequest("Dave Profile", "dave@test.invalid", "5555555555", "Password12345!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("dave@test.invalid", "Password12345!");
        String responseContent = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> loginResponse = objectMapper.readValue(responseContent, Map.class);
        String token = (String) loginResponse.get("accessToken");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dave@test.invalid"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.name").value("Dave Profile"));
    }

    @Test
    @DisplayName("POST /auth/logout invalidates the session")
    void testLogoutSuccess() throws Exception {
        RegisterRequest regReq = new RegisterRequest("Eve Logout", "eve@test.invalid", "4444444444", "Password12345!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("eve@test.invalid", "Password12345!");
        String responseContent = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> loginResponse = objectMapper.readValue(responseContent, Map.class);
        String token = (String) loginResponse.get("accessToken");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."));
    }

    @Test
    @DisplayName("TC-Sec-10: Session inactivity timeout (15 minutes idle triggers 401)")
    void testSessionInactivityTimeout() throws Exception {
        clock.setFixed(Instant.parse("2026-10-27T10:00:00Z"));

        RegisterRequest regReq = new RegisterRequest("Timeout User", "timeout@test.invalid", "5555555555", "Password12345!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("timeout@test.invalid", "Password12345!");
        String responseContent = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> loginResponse = objectMapper.readValue(responseContent, Map.class);
        String token = (String) loginResponse.get("accessToken");

        // Advance 10 minutes: request within 15 min idle threshold touches session and succeeds
        clock.advance(Duration.ofMinutes(10));
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("timeout@test.invalid"));

        // Advance another 10 minutes: request within 15 min idle threshold touches session and succeeds again
        clock.advance(Duration.ofMinutes(10));
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("timeout@test.invalid"));

        // Now idle for 16 minutes (> 15 minutes inactivity timeout)
        clock.advance(Duration.ofMinutes(16));
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }

    @Test
    @DisplayName("TC-Sec-10: Absolute maximum session lifetime (8 hours) terminates active session")
    void testMaxSessionLifetimeEightHours() throws Exception {
        clock.setFixed(Instant.parse("2026-10-27T08:00:00Z"));

        RegisterRequest regReq = new RegisterRequest("MaxLifetime User", "maxlife@test.invalid", "5555555555", "Password12345!");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("maxlife@test.invalid", "Password12345!");
        String responseContent = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> loginResponse = objectMapper.readValue(responseContent, Map.class);
        String token = (String) loginResponse.get("accessToken");

        // Touch every 10 minutes for 7 hours 50 minutes (47 intervals) - all should succeed
        for (int i = 0; i < 47; i++) {
            clock.advance(Duration.ofMinutes(10));
            mockMvc.perform(get("/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        // Advance 20 minutes (now at 8 hours and 10 minutes total from login)
        clock.advance(Duration.ofMinutes(20));

        // Absolute maximum session lifetime (8 hours) reached -> 401 Unauthorized
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }
}
