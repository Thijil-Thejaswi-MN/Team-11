package com.team11.insurance.kyc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.repository.KycRepository;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-05 / INS-F-002 KycController REST Integration Tests")
class KycControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KycRepository kycRepository;

    @BeforeEach
    void setUp() {
        kycRepository.deleteAll();
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(777L, UserRole.CUSTOMER, null));
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("GET /kyc/status for unsubmitted customer returns 200 OK with 'Not Submitted'")
    void testGetStatusUnsubmitted() throws Exception {
        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Not Submitted"));
    }

    @Test
    @DisplayName("POST /kyc submits customer KYC and returns 202 Accepted with 'Pending'")
    void testSubmitKycSuccess() throws Exception {
        Map<String, String> payload = Map.of(
                "panNumber", "ABCDE1234F",
                "aadhaarNumber", "123456789012"
        );

        mockMvc.perform(post("/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.kycStatus").value("Pending"));

        // Subsequent status check returns Pending
        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Pending"));
    }

    @Test
    @DisplayName("POST /kyc with missing panNumber returns 422 Unprocessable Entity")
    void testSubmitKycMissingPanReturns422() throws Exception {
        Map<String, String> payload = Map.of(
                "aadhaarNumber", "123456789012"
        );

        mockMvc.perform(post("/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }
}
