package com.team11.insurance.quotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.RatingRules;
import com.team11.insurance.quotes.dto.HealthMemberRequest;
import com.team11.insurance.quotes.dto.HealthQuoteRequest;
import com.team11.insurance.quotes.dto.QuoteResponse;

/**
 * Unit test suite for {@link HealthQuoteService} (INS-F-006, P1-18, TC-Quote-02).
 */
@DisplayName("Health Quote Service Unit Tests (TC-Quote-02)")
class HealthQuoteServiceTest {

    private IPlanQuery planQuery;
    private QuoteRepository quoteRepository;
    private PlanRatingRuleCache ruleCache;
    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private HealthQuoteService service;

    private Plan healthPlan1;
    private Plan healthPlan2;
    private Plan motorPlan;

    private static final String HEALTH_RULES_JSON =
            "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00},{\"minAge\":36,\"maxAge\":45,\"base\":4500.00},{\"minAge\":46,\"maxAge\":59,\"base\":7000.00},{\"minAge\":60,\"maxAge\":120,\"base\":10000.00}],\"sumInsuredMultipliers\":{\"500000\":1.0,\"1000000\":1.6},\"preExistingMultiplier\":1.2}";

    @BeforeEach
    void setUp() {
        planQuery = mock(IPlanQuery.class);
        quoteRepository = mock(QuoteRepository.class);
        ruleCache = new PlanRatingRuleCache();
        objectMapper = new ObjectMapper();
        fixedClock = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));

        service = new HealthQuoteService(planQuery, quoteRepository, ruleCache, objectMapper, fixedClock);

        healthPlan1 = new Plan("PLAN-A-H1", 1L, "HEALTH", "Health Essential 5L",
                new BigDecimal("500000.00"), BigDecimal.ZERO, 12, new BigDecimal("8500.00"),
                HEALTH_RULES_JSON, null, "[]", true);
        healthPlan1.setId(3L);

        healthPlan2 = new Plan("PLAN-A-H2", 1L, "HEALTH", "Health Advantage 10L",
                new BigDecimal("1000000.00"), BigDecimal.ZERO, 24, new BigDecimal("14000.00"),
                HEALTH_RULES_JSON, null, "[]", true);
        healthPlan2.setId(4L);

        motorPlan = new Plan("PLAN-A-M1", 1L, "MOTOR_FOUR_WHEELER", "Motor Plan",
                new BigDecimal("500000.00"), BigDecimal.ZERO, 0, BigDecimal.ZERO,
                "{}", null, "[]", true);
        motorPlan.setId(1L);

        when(planQuery.getPlan(3L)).thenReturn(healthPlan1);
        when(planQuery.getPlan(4L)).thenReturn(healthPlan2);
        when(planQuery.getPlan(1L)).thenReturn(motorPlan);

        RatingRules rules3 = new RatingRules(3L, "PLAN-A-H1", "R-H", HEALTH_RULES_JSON, null);
        when(planQuery.getRatingRules(3L)).thenReturn(rules3);
        RatingRules rules4 = new RatingRules(4L, "PLAN-A-H2", "R-H", HEALTH_RULES_JSON, null);
        when(planQuery.getRatingRules(4L)).thenReturn(rules4);

        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote q = invocation.getArgument(0);
            try {
                var qIdField = Quote.class.getDeclaredField("id");
                qIdField.setAccessible(true);
                qIdField.set(q, 101L);
            } catch (Exception ignored) {}
            return q;
        });
    }

    @Test
    @DisplayName("TC-Quote-02 Step 1: 1 member age 45, 5 lakh sum insured, pre-existing No -> Rs 4,500.00")
    void tcQuote02Step1() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(45)),
                new BigDecimal("500000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        assertThat(response).isNotNull();
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(response.getBreakdown().getTotalPremium()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("TC-Quote-02 Step 2: 2 members ages 30 & 32, 5 lakh sum insured, pre-existing No -> Rs 6,000.00")
    void tcQuote02Step2() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("500000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        assertThat(response).isNotNull();
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getTotalPremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 3: 2 members ages 30 & 32, 10 lakh sum insured, pre-existing No -> Rs 9,600.00")
    void tcQuote02Step3() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("1000000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        assertThat(response).isNotNull();
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("9600.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(new BigDecimal("3600.00"));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 4: 2 members ages 30 & 32, 5 lakh sum insured, pre-existing Yes -> Rs 7,200.00")
    void tcQuote02Step4() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(30), new HealthMemberRequest(32)),
                new BigDecimal("500000"),
                true
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        assertThat(response).isNotNull();
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("7200.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("TC-Quote-02 Step 5a: No members throws ValidationException")
    void tcQuote02Step5NoMembers() {
        HealthQuoteRequest emptyRequest = new HealthQuoteRequest(
                3L,
                Collections.emptyList(),
                new BigDecimal("500000"),
                false
        );

        assertThatThrownBy(() -> service.calculateHealthQuote(emptyRequest, 1001L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Field 'members' must contain at least one member.");
    }

    @Test
    @DisplayName("TC-Quote-02 Step 5b: Unsupported sum insured throws ValidationException")
    void tcQuote02Step5UnsupportedSumInsured() {
        HealthQuoteRequest unsupportedRequest = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(30)),
                new BigDecimal("750000"),
                false
        );

        assertThatThrownBy(() -> service.calculateHealthQuote(unsupportedRequest, 1001L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not offered by this plan");
    }

    @Test
    @DisplayName("Combined factors: Multi-member, 10 lakh coverage, pre-existing condition")
    void combinedFactors() {
        // Members: age 40 (4500), age 60 (10000) -> base = 14500
        // 10 lakh multiplier: 1.6 -> 23200
        // Pre-existing multiplier: 1.2 -> 27840.00
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(40), new HealthMemberRequest(60)),
                new BigDecimal("1000000"),
                true
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("27840.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("14500.00"));
        assertThat(response.getBreakdown().getTotalPremium()).isEqualByComparingTo(new BigDecimal("27840.00"));
    }

    @Test
    @DisplayName("Member age boundary validation: under 18 and over 120 throw ValidationException")
    void memberAgeBoundaries() {
        HealthQuoteRequest underage = new HealthQuoteRequest(3L, List.of(new HealthMemberRequest(17)), new BigDecimal("500000"), false);
        assertThatThrownBy(() -> service.calculateHealthQuote(underage, 1001L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("below minimum allowable age");

        HealthQuoteRequest overage = new HealthQuoteRequest(3L, List.of(new HealthMemberRequest(121)), new BigDecimal("500000"), false);
        assertThatThrownBy(() -> service.calculateHealthQuote(overage, 1001L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds maximum allowable limit");
    }

    @Test
    @DisplayName("Incompatible plan type (MOTOR) for health quote throws ValidationException")
    void incompatiblePlanTypeThrows() {
        HealthQuoteRequest request = new HealthQuoteRequest(1L, List.of(new HealthMemberRequest(30)), new BigDecimal("500000"), false);
        assertThatThrownBy(() -> service.calculateHealthQuote(request, 1001L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not a health insurance plan");
    }

    @Test
    @DisplayName("Quote persistence verification with 30-day validity")
    void quotePersistence() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(30)),
                new BigDecimal("500000"),
                false
        );

        service.calculateHealthQuote(request, 555L);

        ArgumentCaptor<Quote> captor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository).save(captor.capture());

        Quote saved = captor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(555L);
        assertThat(saved.getPlanId()).isEqualTo(3L);
        assertThat(saved.getProductType()).isEqualTo("HEALTH");
        assertThat(saved.getPremium()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(saved.getValidUntil()).isEqualTo(Instant.parse("2026-10-21T10:00:00Z").plus(java.time.Duration.ofDays(30)));
    }

    @Test
    @DisplayName("Regression: Single member declaring pre-existing disease triggers 1.2x loading")
    void singleMemberDeclaringPreExistingDisease() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(new HealthMemberRequest(45, true)),
                new BigDecimal("500000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        // Member base: 4500. Sum insured 5L: 1.0. Member preExisting: 1.2 -> 5400.00
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("5400.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(response.getBreakdown().getAddOns()).containsEntry("PRE_EXISTING_FACTOR", new BigDecimal("1.2"));
    }

    @Test
    @DisplayName("Regression: Multiple members where only one declares pre-existing condition triggers 1.2x loading")
    void multipleMembersWhereOnlyOneHasCondition() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(
                        new HealthMemberRequest(30, true),
                        new HealthMemberRequest(32, false)
                ),
                new BigDecimal("500000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        // Members base: 3000 + 3000 = 6000. Sum insured: 1.0. PreExisting: 1.2 -> 7200.00
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("7200.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(response.getBreakdown().getAddOns()).containsEntry("PRE_EXISTING_FACTOR", new BigDecimal("1.2"));
    }

    @Test
    @DisplayName("Regression: No member declaring condition with top-level false applies 1.0x factor")
    void noMemberDeclaringCondition() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(
                        new HealthMemberRequest(30, false),
                        new HealthMemberRequest(32, false)
                ),
                new BigDecimal("500000"),
                false
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        // Members base: 3000 + 3000 = 6000. Sum insured: 1.0. PreExisting: 1.0 -> 6000.00
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getBreakdown().getAddOns()).doesNotContainKey("PRE_EXISTING_FACTOR");
    }

    @Test
    @DisplayName("Regression: Top-level pre-existing flag true applies 1.2x loading even when members don't specify")
    void topLevelPreExistingFlagAppliesLoading() {
        HealthQuoteRequest request = new HealthQuoteRequest(
                3L,
                List.of(
                        new HealthMemberRequest(30),
                        new HealthMemberRequest(32)
                ),
                new BigDecimal("500000"),
                true
        );

        QuoteResponse response = service.calculateHealthQuote(request, 1001L);

        // Members base: 6000. PreExisting: 1.2 -> 7200.00
        assertThat(response.getPremium()).isEqualByComparingTo(new BigDecimal("7200.00"));
        assertThat(response.getBreakdown().getBasePremium()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getBreakdown().getAgeLoading()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(response.getBreakdown().getAddOns()).containsEntry("PRE_EXISTING_FACTOR", new BigDecimal("1.2"));
    }
}
