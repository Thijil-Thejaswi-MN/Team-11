package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.entity.Insurer;
import com.team11.insurance.admin.repository.InsurerRepository;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.plans.dto.CreatePlanRequest;
import com.team11.insurance.plans.dto.PatchPlanRequest;
import com.team11.insurance.plans.dto.UpdatePlanRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PlanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private InsurerRepository insurerRepository;

    private Long planAM1Id;
    private Long planAM2Id;
    private Long planAH1Id;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.clearSyntheticUser();

        // Ensure test insurers exist
        if (insurerRepository.count() == 0) {
            insurerRepository.save(new Insurer("SafeShield Insurance"));
            insurerRepository.save(new Insurer("Bharat Care Insurance"));
        }

        // Ensure canonical seeded plans exist
        if (planRepository.count() == 0 
                || planRepository.findByCode("PLAN-A-M1").isEmpty()
                || planRepository.findByCode("PLAN-A-M2").isEmpty()
                || planRepository.findByCode("PLAN-A-H1").isEmpty()) {
            planRepository.deleteAll();
            new PlanDataSeeder(planRepository).run();
        }

        // Retrieve seeded plans from PlanDataSeeder
        Plan am1 = planRepository.findByCode("PLAN-A-M1").orElse(null);
        if (am1 != null) {
            planAM1Id = am1.getId();
        }
        Plan am2 = planRepository.findByCode("PLAN-A-M2").orElse(null);
        if (am2 != null) {
            planAM2Id = am2.getId();
        }
        Plan ah1 = planRepository.findByCode("PLAN-A-H1").orElse(null);
        if (ah1 != null) {
            planAH1Id = ah1.getId();
        }
    }

    @Test
    @DisplayName("TC-Plan-01 Step 1 & 2: Insurer A Admin creates plan and it appears in public browse")
    void testTCPlan01_Step1And2_CreateAndBrowsePlan() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN); // Insurer 1

        String uniqueCode = "PLAN-A-TEST-" + System.currentTimeMillis();
        CreatePlanRequest createReq = new CreatePlanRequest(
                uniqueCode, "MOTOR_FOUR_WHEELER", "SafeShield Test Car Plan",
                new BigDecimal("600000.00"), new BigDecimal("1000.00"), 0,
                new BigDecimal("3500.00"), "{\"type\":\"R-M\"}", "{}", List.of("ZERO_DEP"), true
        );

        // Step 1: POST /plans
        String createRes = mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").isNumber())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> map = objectMapper.readValue(createRes, Map.class);
        Long createdPlanId = ((Number) map.get("planId")).longValue();

        // Step 2: Public GET /plans without token -> verify plan appears
        TestCurrentUserProvider.setUnauthenticated();
        mockMvc.perform(get("/plans?type=4W"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == '" + uniqueCode + "')].planId").value(createdPlanId.intValue()))
                .andExpect(jsonPath("$.items[?(@.code == '" + uniqueCode + "')].name").value("SafeShield Test Car Plan"))
                .andExpect(jsonPath("$.items[?(@.code == '" + uniqueCode + "')].coverage").value(600000.00))
                .andExpect(jsonPath("$.items[?(@.code == '" + uniqueCode + "')].deductible").value(1000.00))
                .andExpect(jsonPath("$.items[?(@.code == '" + uniqueCode + "')].addOns[0]").value("ZERO_DEP"));
    }

    @Test
    @DisplayName("TC-Plan-01 Step 3: Deactivate plan via PATCH and verify it is removed from public browse")
    void testTCPlan01_Step3_DeactivatePlan() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN); // Insurer 1

        String codeToDeactivate = "PLAN-A-DEACT-" + System.currentTimeMillis();
        CreatePlanRequest createReq = new CreatePlanRequest(
                codeToDeactivate, "4W", "To Be Deactivated",
                new BigDecimal("400000.00"), BigDecimal.ZERO, 0,
                new BigDecimal("2000.00"), "{}", "{}", List.of(), true
        );

        String createRes = mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long planId = ((Number) objectMapper.readValue(createRes, Map.class).get("planId")).longValue();

        // Step 3: PATCH /plans/{id} { active: false }
        PatchPlanRequest patchReq = new PatchPlanRequest(false);
        mockMvc.perform(patch("/plans/" + planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.active").value(false));

        // GET /plans again -> plan should NOT appear in active list
        TestCurrentUserProvider.setUnauthenticated();
        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.code == '" + codeToDeactivate + "')]").doesNotExist());
    }

    @Test
    @DisplayName("TC-Plan-01 Step 4: Insurer B Admin denied from modifying Insurer A's plan (403 ACCESS-403)")
    void testTCPlan01_Step4_CrossInsurerModificationDenied() throws Exception {
        // Insurer B Admin has insurerId = 2
        CurrentUser insurerBAdmin = new CurrentUser(2002L, UserRole.INSURER_ADMIN, 2L);
        TestCurrentUserProvider.setSyntheticUser(insurerBAdmin);

        UpdatePlanRequest updateReq = new UpdatePlanRequest();
        updateReq.setName("Unauthorized Modification");
        updateReq.setType("MOTOR_FOUR_WHEELER");
        updateReq.setCoverage(new BigDecimal("500000.00"));
        updateReq.setDeductible(BigDecimal.ZERO);
        updateReq.setBasePremium(new BigDecimal("2000.00"));
        updateReq.setActive(true);

        // Attempting to update Insurer A's plan (planAM1Id, insurerId = 1)
        mockMvc.perform(put("/plans/" + planAM1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"))
                .andExpect(jsonPath("$.message", containsString("insurer isolation")));
    }

    @Test
    @DisplayName("TC-Plan-01 Step 5: Invalid plan values (negative coverage/deductible) rejected with HTTP 400/422")
    void testTCPlan01_Step5_InvalidValuesRejected() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN);

        CreatePlanRequest invalidReq = new CreatePlanRequest(
                "PLAN-INVALID", "4W", "Invalid Plan",
                new BigDecimal("-500.00"), new BigDecimal("-100.00"), 0,
                new BigDecimal("-200.00"), "{}", "{}", List.of(), true
        );

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }

    @Test
    @DisplayName("TC-Plan-02 Step 1: Public GET /plans?type=HEALTH without login returns active health plans")
    void testTCPlan02_Step1_PublicBrowseHealthPlans() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/plans?type=HEALTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].type").value("HEALTH"))
                .andExpect(jsonPath("$.items[0].coverage").isNumber())
                .andExpect(jsonPath("$.items[0].basePremium").isNumber());
    }

    @Test
    @DisplayName("TC-Plan-02 Step 2: Public GET /plans/compare with 3 plans returns comparison table")
    void testTCPlan02_Step2_CompareUpToThreePlans() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/plans/compare?ids=" + planAM1Id + "," + planAM2Id + "," + planAH1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans", hasSize(3)))
                .andExpect(jsonPath("$.plans[0].code").isNotEmpty())
                .andExpect(jsonPath("$.plans[0].coverage").isNumber())
                .andExpect(jsonPath("$.plans[0].deductible").isNumber())
                .andExpect(jsonPath("$.plans[0].basePremium").isNumber());
    }

    @Test
    @DisplayName("TC-Plan-02 Step 3: Public GET /plans/compare with 4 plans rejected with HTTP 400 Bad Request")
    void testTCPlan02_Step3_CompareFourPlansRejected() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/plans/compare?ids=1,2,3,4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message", containsString("Cannot compare more than 3 plans")));
    }

    @Test
    @DisplayName("TC-Plan-02 Step 4: Public GET /plans/compare with unknown plan ID returns HTTP 404 NOTFOUND-404")
    void testTCPlan02_Step4_CompareUnknownPlanReturns404() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/plans/compare?ids=" + planAM1Id + ",99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("NOTFOUND-404"));
    }

    @Test
    @DisplayName("Non-admin role (Customer) cannot create plans (403 ACCESS-403)")
    void testCustomerCannotCreatePlan() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        CreatePlanRequest createReq = new CreatePlanRequest(
                "PLAN-CUST-FORBIDDEN", "4W", "Forbidden Plan",
                new BigDecimal("500000.00"), BigDecimal.ZERO, 0,
                new BigDecimal("2000.00"), "{}", "{}", List.of(), true
        );

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }
}
