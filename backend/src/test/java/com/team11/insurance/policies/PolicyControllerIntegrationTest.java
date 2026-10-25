package com.team11.insurance.policies;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P3-05 PolicyController REST Integration Tests")
class PolicyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyService policyService;

    private Policy testPolicy;
    private final Long customerAId = 501L;
    private final Long customerBId = 502L;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.clearSyntheticUser();
        policyRepository.deleteAll();

        testPolicy = new Policy();
        testPolicy.setPolicyNo("POL-2026-000501");
        testPolicy.setApplicationId(7001L);
        testPolicy.setCustomerId(customerAId);
        testPolicy.setInsurerId(1L);
        testPolicy.setPlanId(10L);
        testPolicy.setVehicleRegNo("KA-01-AB-1234");
        testPolicy.setPremiumPaid(new BigDecimal("9480.00"));
        testPolicy.setStartDate(LocalDate.of(2026, 10, 23));
        testPolicy.setEndDate(LocalDate.of(2027, 10, 22));
        testPolicy.setStatus(PolicyStatus.ACTIVE);
        testPolicy = policyRepository.save(testPolicy);
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
        policyRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /policies returns only policies owned by authenticated customer")
    void testGetPolicies_CustomerOwnPolicies() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerAId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].policyNo", is("POL-2026-000501")))
                .andExpect(jsonPath("$[0].customerId", is(customerAId.intValue())))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[0].vehicleRegNo", is("KA-01-AB-1234")));
    }

    @Test
    @DisplayName("GET /policies isolates customer data: other customer sees empty list")
    void testGetPolicies_CustomerIsolation() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerBId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /policies/{id} returns single policy details for owner")
    void testGetPolicyById_Success() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerAId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies/{id}", testPolicy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNo", is("POL-2026-000501")))
                .andExpect(jsonPath("$.premiumPaid", is(9480.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("GET /policies/{id} rejects access from other customers with 403 ACCESS-403")
    void testGetPolicyById_ForbiddenOtherCustomer() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerBId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies/{id}", testPolicy.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /policies/{id} returns 404 for non-existent policy ID")
    void testGetPolicyById_NotFound() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerAId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /policies/{id}/document downloads OpenPDF policy schedule with correct headers")
    void testGetPolicyDocument_Success() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerAId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies/{id}/document", testPolicy.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("inline; filename=")));
    }

    @Test
    @DisplayName("GET /policies/{id}/document rejects unauthorized download with 403")
    void testGetPolicyDocument_ForbiddenOtherCustomer() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(customerBId, UserRole.CUSTOMER, null));

        mockMvc.perform(get("/policies/{id}/document", testPolicy.getId()))
                .andExpect(status().isForbidden());
    }
}
