package com.team11.insurance.claims;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ClaimController REST Integration Tests")
class ClaimControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Policy activePolicyInsurer1;
    private Policy inactivePolicyInsurer1;
    private Claim existingClaimInsurer1;

    @BeforeEach
    void setUp() {
        activePolicyInsurer1 = new Policy();
        activePolicyInsurer1.setPolicyNo("POL-TEST-001");
        activePolicyInsurer1.setCustomerId(1001L);
        activePolicyInsurer1.setInsurerId(1L);
        activePolicyInsurer1.setPlanId(1L);
        activePolicyInsurer1.setPremiumPaid(new BigDecimal("12000.00"));
        activePolicyInsurer1.setStartDate(LocalDate.now().minusMonths(2));
        activePolicyInsurer1.setEndDate(LocalDate.now().plusMonths(10));
        activePolicyInsurer1.setStatus(PolicyStatus.ACTIVE);
        activePolicyInsurer1 = policyRepository.saveAndFlush(activePolicyInsurer1);

        inactivePolicyInsurer1 = new Policy();
        inactivePolicyInsurer1.setPolicyNo("POL-TEST-002");
        inactivePolicyInsurer1.setCustomerId(1001L);
        inactivePolicyInsurer1.setInsurerId(1L);
        inactivePolicyInsurer1.setPlanId(1L);
        inactivePolicyInsurer1.setPremiumPaid(new BigDecimal("12000.00"));
        inactivePolicyInsurer1.setStartDate(LocalDate.now().minusYears(2));
        inactivePolicyInsurer1.setEndDate(LocalDate.now().minusYears(1));
        inactivePolicyInsurer1.setStatus(PolicyStatus.EXPIRED);
        inactivePolicyInsurer1 = policyRepository.saveAndFlush(inactivePolicyInsurer1);

        existingClaimInsurer1 = new Claim();
        existingClaimInsurer1.setClaimNo("CLM-TEST-001");
        existingClaimInsurer1.setPolicyId(activePolicyInsurer1.getId());
        existingClaimInsurer1.setPolicyNo(activePolicyInsurer1.getPolicyNo());
        existingClaimInsurer1.setCustomerId(1001L);
        existingClaimInsurer1.setInsurerId(1L);
        existingClaimInsurer1.setIncidentDate(LocalDate.now().minusDays(5));
        existingClaimInsurer1.setClaimAmount(new BigDecimal("8000.00"));
        existingClaimInsurer1.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);
        existingClaimInsurer1 = claimRepository.saveAndFlush(existingClaimInsurer1);
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    // =========================================================================
    // POST /api/v1/claims
    // =========================================================================

    @Test
    @DisplayName("POST /claims: Customer files valid claim -> 201 Created")
    void fileClaim_success_returns201() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER); // id: 1001

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                activePolicyInsurer1.getId(),
                LocalDate.now().minusDays(3),
                new BigDecimal("6000.00"),
                "Front bumper damage",
                List.of(101L)
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").isNumber())
                .andExpect(jsonPath("$.claimNo").value(containsString("CLM-")))
                .andExpect(jsonPath("$.status").value("DOCUMENT_VERIFICATION"))
                .andExpect(jsonPath("$.claimedAmount").value(6000.00))
                .andExpect(jsonPath("$.duplicateFlag").value(false));
    }

    @Test
    @DisplayName("POST /claims: Fails closed when unauthenticated -> 401/403")
    void fileClaim_unauthenticated_failsClosed() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                activePolicyInsurer1.getId(),
                LocalDate.now().minusDays(3),
                new BigDecimal("6000.00"),
                "Damage",
                List.of()
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /claims: Fails with 404 when policy does not exist")
    void fileClaim_policyNotFound_returns404() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                99999L,
                LocalDate.now().minusDays(3),
                new BigDecimal("6000.00"),
                "Damage",
                List.of()
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTFOUND-404"));
    }

    @Test
    @DisplayName("POST /claims: Fails with 409 STATE-409 when policy is inactive")
    void fileClaim_policyInactive_returns409() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                inactivePolicyInsurer1.getId(),
                LocalDate.now().minusDays(3),
                new BigDecimal("6000.00"),
                "Damage",
                List.of()
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE-409"))
                .andExpect(jsonPath("$.message").value(containsString("Policy is not active")));
    }

    @Test
    @DisplayName("POST /claims: Fails with 422 VALID-422 when incident date > 30 days ago")
    void fileClaim_incidentDateTooOld_returns422() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                activePolicyInsurer1.getId(),
                LocalDate.now().minusDays(35),
                new BigDecimal("6000.00"),
                "Old damage",
                List.of()
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message").value(containsString("within 30 days")));
    }

    @Test
    @DisplayName("POST /claims: Fails with 403 when customer does not own policy")
    void fileClaim_notOwner_returns403() throws Exception {
        CurrentUser otherCustomer = new CurrentUser(1002L, UserRole.CUSTOMER, null);
        TestCurrentUserProvider.setSyntheticUser(otherCustomer);

        ClaimSubmissionRequest request = new ClaimSubmissionRequest(
                activePolicyInsurer1.getId(),
                LocalDate.now().minusDays(3),
                new BigDecimal("6000.00"),
                "Damage",
                List.of()
        );

        mockMvc.perform(post("/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    // =========================================================================
    // GET /api/v1/claims/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /claims/{id}: Customer accesses own claim -> 200 OK")
    void getClaim_owner_returns200() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER); // id: 1001

        mockMvc.perform(get("/claims/" + existingClaimInsurer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingClaimInsurer1.getId()))
                .andExpect(jsonPath("$.claimNo").value("CLM-TEST-001"))
                .andExpect(jsonPath("$.customerId").value(1001))
                .andExpect(jsonPath("$.status").value("DOCUMENT_VERIFICATION"));
    }

    @Test
    @DisplayName("GET /claims/{id}: Another customer accessing claim -> 403 Forbidden")
    void getClaim_otherCustomer_returns403() throws Exception {
        CurrentUser otherCustomer = new CurrentUser(1002L, UserRole.CUSTOMER, null);
        TestCurrentUserProvider.setSyntheticUser(otherCustomer);

        mockMvc.perform(get("/claims/" + existingClaimInsurer1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("GET /claims/{id}: Claims Officer of same insurer accesses claim -> 200 OK")
    void getClaim_matchingInsurerClaimsOfficer_returns200() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CLAIMS_OFFICER); // insurerId: 1

        mockMvc.perform(get("/claims/" + existingClaimInsurer1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingClaimInsurer1.getId()))
                .andExpect(jsonPath("$.insurerId").value(1));
    }

    @Test
    @DisplayName("GET /claims/{id}: Claims Officer of different insurer -> 403 Forbidden (isolation)")
    void getClaim_differentInsurerClaimsOfficer_returns403() throws Exception {
        CurrentUser officerInsurer2 = new CurrentUser(4002L, UserRole.CLAIMS_OFFICER, 2L);
        TestCurrentUserProvider.setSyntheticUser(officerInsurer2);

        mockMvc.perform(get("/claims/" + existingClaimInsurer1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("GET /claims/{id}: Missing currentUser fails closed -> 401/403")
    void getClaim_unauthenticated_failsClosed() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/claims/" + existingClaimInsurer1.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /claims/{id}: Not found claim -> 404")
    void getClaim_notFound_returns404() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        mockMvc.perform(get("/claims/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTFOUND-404"));
    }
}
