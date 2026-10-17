package com.team11.insurance.kyc;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.entity.KycRecord;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-05 / INS-F-002 KycController REST Integration & Security Tests")
class KycControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("GET /kyc/status for verified customer returns 200 OK with 'Verified'")
    void testGetStatusVerifiedCustomer() throws Exception {
        KycRecord record = new KycRecord(777L, "ABCDE1234F", "123456789012", KycStatus.VERIFIED);
        kycRepository.saveAndFlush(record);

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Verified"));
    }

    @Test
    @DisplayName("Finding 3: Unauthenticated caller to GET /kyc/status is rejected with 401 Unauthorized")
    void testGetStatusUnauthenticatedRejected() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }

    @Test
    @DisplayName("Finding 3: INSURER_ADMIN caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusInsurerAdminRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(101L, UserRole.INSURER_ADMIN, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: UNDERWRITER caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusUnderwriterRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(102L, UserRole.UNDERWRITER, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: CLAIMS_OFFICER caller to GET /kyc/status is rejected with 403 Forbidden")
    void testGetStatusClaimsOfficerRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(103L, UserRole.CLAIMS_OFFICER, 1L));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: SYSTEM_ADMIN caller to GET /kyc/status is rejected under current contract with 403 Forbidden")
    void testGetStatusSystemAdminRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(1L, UserRole.SYSTEM_ADMIN, null));

        mockMvc.perform(get("/kyc/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("Finding 3: Client cannot retrieve another customer's KYC status by supplying alternative user ID")
    void testCrossCustomerAccessDeniedViaAlternativeId() throws Exception {
        // Customer 777 has PENDING status
        kycRepository.saveAndFlush(new KycRecord(777L, "ABCDE1234F", "123456789012", KycStatus.PENDING));

        // Victim customer 888 has VERIFIED status
        kycRepository.saveAndFlush(new KycRecord(888L, "XYZPA5678K", "987654321098", KycStatus.VERIFIED));

        // Attacker Customer 777 tries to pass userId=888 in query param and header
        mockMvc.perform(get("/kyc/status")
                        .param("userId", "888")
                        .header("X-User-Id", "888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("Pending")); // Strictly returns authenticated customer's own status (777), not 888!
    }

    @Test
    @DisplayName("Finding 2 (Path B): Incomplete POST /kyc endpoint is deferred and not exposed (404 Not Found)")
    void testPostKycEndpointIsNotExposed() throws Exception {
        mockMvc.perform(post("/kyc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"panNumber\":\"ABCDE1234F\",\"aadhaarNumber\":\"123456789012\"}"))
                .andExpect(status().isNotFound());
    }
}
