package com.team11.insurance.quotes;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.quotes.dto.HealthMemberRequest;
import com.team11.insurance.quotes.dto.HealthQuoteRequest;

/**
 * End-to-end integration tests for {@link QuoteController} health quote endpoint (INS-F-006, P1-18).
 * Specifically verifies all acceptance criteria for TC-Quote-02:
 * - Step 1: 1 member age 45, 5 lakh sum insured, pre-existing No -> 200 OK, premium 4,500.00
 * - Step 2: 2 members ages 30 & 32, 5 lakh sum insured, pre-existing No -> 200 OK, premium 6,000.00
 * - Step 3: 2 members ages 30 & 32, 10 lakh sum insured, pre-existing No -> 200 OK, premium 9,600.00
 * - Step 4: 2 members ages 30 & 32, 5 lakh sum insured, pre-existing Yes -> 200 OK, premium 7,200.00
 * - Step 5: Invalid inputs (no members, unsupported sum insured, underage) -> HTTP 422 VALID-422
 * - Server-side premium integrity: client-supplied premium cannot manipulate calculation
 * - Role enforcement: only authenticated customers can generate quotes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P1-18 Health Quote Controller Integration Tests (TC-Quote-02)")
class HealthQuoteControllerIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        public Clock fixedHealthTestClock() {
            return Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    private Long healthPlanId;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.clearSyntheticUser();
        quoteRepository.deleteAll();

        // Locate seeded PLAN-A-H1 (Health Essential 5L)
        Plan plan = planRepository.findByCode("PLAN-A-H1")
                .orElseThrow(() -> new IllegalStateException("Seeded plan PLAN-A-H1 not found in test db"));
        this.healthPlanId = plan.getId();
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
        quoteRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-Quote-02 Step 1: 1 member age 45, 5 lakh sum insured, preExisting No -> Rs 4,500")
    void tcQuote02Step1() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(45)),
                new BigDecimal("500000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", notNullValue()))
                .andExpect(jsonPath("$.premium", is(4500.00)))
                .andExpect(jsonPath("$.breakdown.basePremium", is(4500.00)))
                .andExpect(jsonPath("$.breakdown.totalPremium", is(4500.00)))
                .andExpect(jsonPath("$.breakdown.ageLoading", is(0.00)))
                .andExpect(jsonPath("$.breakdown.addOns.SUM_INSURED_FACTOR", is(1.0)))
                .andExpect(jsonPath("$.validUntil", notNullValue()));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 2: 2 members ages 30 & 32, 5 lakh sum insured, preExisting No -> Rs 6,000")
    void tcQuote02Step2() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("500000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", notNullValue()))
                .andExpect(jsonPath("$.premium", is(6000.00)))
                .andExpect(jsonPath("$.breakdown.basePremium", is(6000.00)))
                .andExpect(jsonPath("$.breakdown.totalPremium", is(6000.00)));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 3: 2 members ages 30 & 32, 10 lakh sum insured, preExisting No -> Rs 9,600")
    void tcQuote02Step3() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("1000000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", notNullValue()))
                .andExpect(jsonPath("$.premium", is(9600.00)))
                .andExpect(jsonPath("$.breakdown.basePremium", is(6000.00)))
                .andExpect(jsonPath("$.breakdown.ageLoading", is(3600.00)))
                .andExpect(jsonPath("$.breakdown.addOns.SUM_INSURED_FACTOR", is(1.6)));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 4: 2 members ages 30 & 32, 5 lakh sum insured, preExisting Yes -> Rs 7,200")
    void tcQuote02Step4() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("500000"),
                true
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId", notNullValue()))
                .andExpect(jsonPath("$.premium", is(7200.00)))
                .andExpect(jsonPath("$.breakdown.basePremium", is(6000.00)))
                .andExpect(jsonPath("$.breakdown.ageLoading", is(1200.00)))
                .andExpect(jsonPath("$.breakdown.addOns.PRE_EXISTING_FACTOR", is(1.2)));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 5a: Request with empty members returns HTTP 422 VALID-422")
    void tcQuote02Step5EmptyMembersReturns422() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(),
                new BigDecimal("500000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")))
                .andExpect(jsonPath("$.message", containsString("Field 'members' must contain at least one member.")));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 5b: Unsupported sum insured (Rs 7,50,000) returns HTTP 422 VALID-422")
    void tcQuote02Step5UnsupportedSumInsuredReturns422() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(30)),
                new BigDecimal("750000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")))
                .andExpect(jsonPath("$.message", containsString("not offered by this plan")));
    }

    @Test
    @DisplayName("Validation: Member under 18 years returns HTTP 422 VALID-422")
    void memberUnderageReturns422() throws Exception {
        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(16)),
                new BigDecimal("500000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")))
                .andExpect(jsonPath("$.message", containsString("minimum allowable age")));
    }

    @Test
    @DisplayName("Server-side premium integrity: Client-supplied premium is ignored")
    void clientSuppliedPremiumIgnored() throws Exception {
        Map<String, Object> payload = Map.of(
                "planId", healthPlanId,
                "members", List.of(Map.of("age", 45)),
                "sumInsured", 500000,
                "preExisting", false,
                "premium", 99.00 // Malicious attempt to override
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium", is(4500.00))); // Server rating rule prevails
    }

    @Test
    @DisplayName("Role enforcement: Non-customer (UNDERWRITER) is rejected with HTTP 403 ACCESS-403")
    void nonCustomerRoleForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        HealthQuoteRequest request = new HealthQuoteRequest(
                healthPlanId,
                List.of(new HealthMemberRequest(30)),
                new BigDecimal("500000"),
                false
        );

        mockMvc.perform(post("/quotes/health")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode", is("ACCESS-403")));
    }
}
