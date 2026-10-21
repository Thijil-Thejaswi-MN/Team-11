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
import com.team11.insurance.quotes.dto.MotorQuoteRequest;

/**
 * End-to-end integration tests for {@link QuoteController} and Motor Quote Engine (P1-06).
 * Specifically verifies all acceptance criteria for TC-Quote-01:
 * - Step 1: IDV 4,00,000, regYear 2019, add-on ZERO_DEP -> 200 OK, premium 9,480.00
 * - Step 2: IDV 4,00,000, regYear 2024, no add-ons -> 200 OK, premium 8,000.00
 * - Step 3: IDV 6,00,000, regYear 2024, no add-ons -> 200 OK, premium 12,000.00
 * - Step 4: Invalid vehicle input / future regYear / negative IDV -> HTTP 422 VALID-422
 * - Server-side premium integrity: client cannot manipulate total premium
 * - Role enforcement: only authenticated customers can generate quotes
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P1-06 Motor Quote Controller Integration Tests (TC-Quote-01)")
class QuoteControllerIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        public Clock fixedTestClock() {
            // Fixed Sprint 1 Day 1: 19 October 2026
            return Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneId.of("UTC"));
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

    private Long fourWheelerPlanId;
    private Long twoWheelerPlanId;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.clearSyntheticUser();
        quoteRepository.deleteAll();

        // Retrieve seeded STP Section 6 plans
        Plan plan4W = planRepository.findByCode("PLAN-A-M1")
                .orElseThrow(() -> new IllegalStateException("Seeded PLAN-A-M1 not found"));
        fourWheelerPlanId = plan4W.getId();

        Plan plan2W = planRepository.findByCode("PLAN-A-M2")
                .orElseThrow(() -> new IllegalStateException("Seeded PLAN-A-M2 not found"));
        twoWheelerPlanId = plan2W.getId();
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
        quoteRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-Quote-01 Step 1: IDV 400000, regYear 2019 (age 7), ZERO_DEP -> premium 9480")
    void testTCQuote01_Step1_Age7WithZeroDep() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Maruti", "Swift", 2019, "Belagavi",
                new BigDecimal("400000"), List.of("ZERO_DEP")
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").isNumber())
                .andExpect(jsonPath("$.premium").value(9480.00))
                .andExpect(jsonPath("$.breakdown.basePremium").value(8000.00))
                .andExpect(jsonPath("$.breakdown.ageLoading").value(400.00))
                .andExpect(jsonPath("$.breakdown.addOns.ZERO_DEP").value(1080.00))
                .andExpect(jsonPath("$.breakdown.totalPremium").value(9480.00))
                .andExpect(jsonPath("$.validUntil").isNotEmpty());

        // Verify entity persisted in repository
        List<Quote> savedQuotes = quoteRepository.findByCustomerId(1001L);
        assertEquals(1, savedQuotes.size());
        assertEquals(new BigDecimal("9480.00"), savedQuotes.get(0).getPremium());
    }

    @Test
    @DisplayName("TC-Quote-01 Step 2: IDV 400000, regYear 2024 (age 2), no add-ons -> premium 8000")
    void testTCQuote01_Step2_Age2NoAddOns() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Maruti", "Swift", 2024, "Belagavi",
                new BigDecimal("400000"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(8000.00))
                .andExpect(jsonPath("$.breakdown.basePremium").value(8000.00))
                .andExpect(jsonPath("$.breakdown.ageLoading").value(0.00))
                .andExpect(jsonPath("$.breakdown.totalPremium").value(8000.00));
    }

    @Test
    @DisplayName("TC-Quote-01 Step 3: IDV 600000, regYear 2024 (age 2), no add-ons -> premium 12000")
    void testTCQuote01_Step3_HigherIdvAge2() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Hyundai", "Creta", 2024, "Bengaluru",
                new BigDecimal("600000"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(12000.00))
                .andExpect(jsonPath("$.breakdown.basePremium").value(12000.00))
                .andExpect(jsonPath("$.breakdown.ageLoading").value(0.00))
                .andExpect(jsonPath("$.breakdown.totalPremium").value(12000.00));
    }

    @Test
    @DisplayName("TC-Quote-01 Step 4a: Future regYear 2030 returns HTTP 422 with VALID-422 error code")
    void testTCQuote01_Step4a_FutureRegYearReturns422() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Maruti", "Swift", 2030, "Belagavi",
                new BigDecimal("400000"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message", containsString("future")));
    }

    @Test
    @DisplayName("TC-Quote-01 Step 4b: Negative or zero IDV returns HTTP 422 with VALID-422 error code")
    void testTCQuote01_Step4b_NegativeIdvReturns422() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Maruti", "Swift", 2020, "Belagavi",
                new BigDecimal("-500"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message", containsString("idv")));
    }

    @Test
    @DisplayName("Two-Wheeler quote calculates 2% base and PLAN-A-M2 ZERO_DEP 500 flat fee")
    void testTwoWheelerQuote() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                twoWheelerPlanId, "2W", "Hero", "Splendor", 2024, "Belagavi",
                new BigDecimal("80000"), List.of("ZERO_DEP")
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(2100.00)) // 1600 base + 0 loading + 500 ZERO_DEP
                .andExpect(jsonPath("$.breakdown.basePremium").value(1600.00))
                .andExpect(jsonPath("$.breakdown.addOns.ZERO_DEP").value(500.00))
                .andExpect(jsonPath("$.breakdown.totalPremium").value(2100.00));
    }

    @Test
    @DisplayName("Client-supplied premium cannot override server-calculated premium")
    void testClientSuppliedPremiumIgnored() throws Exception {
        Map<String, Object> payload = Map.of(
                "planId", fourWheelerPlanId,
                "vehicleType", "4W",
                "make", "Maruti",
                "model", "Swift",
                "regYear", 2019,
                "city", "Belagavi",
                "idv", 400000,
                "addOns", List.of("ZERO_DEP"),
                "premium", 1.00 // Client tampering attempt
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premium").value(9480.00)) // Correctly computed server-side
                .andExpect(jsonPath("$.breakdown.totalPremium").value(9480.00));
    }

    @Test
    @DisplayName("Non-existent plan returns HTTP 404 NOTFOUND-404")
    void testNonExistentPlanReturns404() throws Exception {
        MotorQuoteRequest request = new MotorQuoteRequest(
                99999L, "4W", "Maruti", "Swift", 2020, "Belagavi",
                new BigDecimal("400000"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("NOTFOUND-404"));
    }

    @Test
    @DisplayName("Non-customer role (e.g. Underwriter) is denied access with HTTP 403 ACCESS-403")
    void testNonCustomerRoleDenied() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        MotorQuoteRequest request = new MotorQuoteRequest(
                fourWheelerPlanId, "4W", "Maruti", "Swift", 2020, "Belagavi",
                new BigDecimal("400000"), List.of()
        );

        mockMvc.perform(post("/quotes/motor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }
}
