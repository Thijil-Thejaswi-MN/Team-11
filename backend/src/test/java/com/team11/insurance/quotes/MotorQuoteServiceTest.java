package com.team11.insurance.quotes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.RatingRules;
import com.team11.insurance.quotes.dto.MotorQuoteRequest;
import com.team11.insurance.quotes.dto.QuoteResponse;

/**
 * Unit tests for {@link MotorQuoteService} verifying:
 * - TC-Quote-01 formula: base = IDV * 0.02, ageLoading = base * 0.05 for age in [6, 10], ZERO_DEP = 1080.00
 * - Injected clock behavior and anniversary boundary dates
 * - Invalid IDV and vehicle input validation (throwing ValidationException)
 * - Server-side premium integrity (client-supplied values ignored)
 */
@ExtendWith(MockitoExtension.class)
class MotorQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private IPlanQuery planQuery;

    private PlanRatingRuleCache ruleCache;
    private ObjectMapper objectMapper;
    private Clock fixedClock;
    private MotorQuoteService quoteService;

    // Fixed test clock at 2026-10-19T00:00:00Z (Sprint 1 Day 1)
    private static final Instant TEST_INSTANT_2026 = Instant.parse("2026-10-19T00:00:00Z");
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private Plan fourWheelerPlan;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ruleCache = new PlanRatingRuleCache();
        fixedClock = Clock.fixed(TEST_INSTANT_2026, TEST_ZONE);

        quoteService = new MotorQuoteService(
                quoteRepository,
                planQuery,
                ruleCache,
                fixedClock,
                objectMapper
        );

        // Canonical PLAN-A-M1 fixture from PlanDataSeeder
        fourWheelerPlan = new Plan(
                "PLAN-A-M1",
                1L,
                "MOTOR_FOUR_WHEELER",
                "SafeShield Comprehensive Car",
                new BigDecimal("500000.00"),
                new BigDecimal("2000.00"),
                0,
                BigDecimal.ZERO,
                "{\"type\":\"R-M\",\"ownDamageBasePct\":0.02,\"ageLoadingMinYears\":6,\"ageLoadingMaxYears\":10,\"ageLoadingPct\":0.05,\"addOns\":{\"ZERO_DEP\":1080.00},\"depreciationPct\":0.10}",
                "{\"maxVehicleAgeYears\":20,\"referralVehicleAgeYears\":15}",
                "[\"ZERO_DEP\",\"ROADSIDE_ASSISTANCE\"]",
                true
        );
        // Simulate JPA ID assignment
        try {
            var idField = Plan.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(fourWheelerPlan, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        lenient().when(planQuery.getPlan(1L)).thenReturn(fourWheelerPlan);
        lenient().when(planQuery.getRatingRules(1L)).thenReturn(new RatingRules(
                1L,
                "PLAN-A-M1",
                "R-M",
                fourWheelerPlan.getPremiumRulesJson(),
                fourWheelerPlan.getUnderwritingRulesJson()
        ));

        lenient().when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote q = invocation.getArgument(0);
            try {
                var qIdField = Quote.class.getDeclaredField("id");
                qIdField.setAccessible(true);
                qIdField.set(q, 5021L);
            } catch (Exception ignored) {}
            return q;
        });
    }

    @Test
    @DisplayName("TC-Quote-01 Step 1: IDV 400000, regYear 2019 (age 7), ZERO_DEP -> total Rs 9480")
    void testTCQuote01Step1_VehicleAge7WithZeroDep() {
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2019, "Belagavi",
                new BigDecimal("400000.00"), List.of("ZERO_DEP")
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        assertNotNull(response);
        assertEquals(5021L, response.getQuoteId());
        assertEquals(new BigDecimal("9480.00"), response.getPremium());

        var breakdown = response.getBreakdown();
        assertNotNull(breakdown);
        assertEquals(new BigDecimal("8000.00"), breakdown.getBasePremium());
        assertEquals(new BigDecimal("400.00"), breakdown.getAgeLoading());
        assertEquals(new BigDecimal("1080.00"), breakdown.getAddOns().get("ZERO_DEP"));
        assertEquals(new BigDecimal("9480.00"), breakdown.getTotalPremium());
    }

    @Test
    @DisplayName("TC-Quote-01 Step 2: IDV 400000, regYear 2024 (age 2), no add-ons -> total Rs 8000")
    void testTCQuote01Step2_VehicleAge2NoAddOns() {
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2024, "Belagavi",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        assertEquals(new BigDecimal("8000.00"), response.getPremium());
        assertEquals(new BigDecimal("8000.00"), response.getBreakdown().getBasePremium());
        assertEquals(new BigDecimal("0.00"), response.getBreakdown().getAgeLoading());
        assertTrue(response.getBreakdown().getAddOns().isEmpty());
    }

    @Test
    @DisplayName("TC-Quote-01 Step 3: IDV 600000, regYear 2024 (age 2), no add-ons -> total Rs 12000")
    void testTCQuote01Step3_HigherIdvVehicleAge2NoAddOns() {
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Hyundai", "Creta", 2024, "Bengaluru",
                new BigDecimal("600000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        assertEquals(new BigDecimal("12000.00"), response.getPremium());
        assertEquals(new BigDecimal("12000.00"), response.getBreakdown().getBasePremium());
        assertEquals(new BigDecimal("0.00"), response.getBreakdown().getAgeLoading());
    }

    @Test
    @DisplayName("Boundary: Exactly 6 years old receives 5% age loading")
    void testBoundary_VehicleAgeExactly6Years() {
        // In 2026, regYear 2020 gives age 6
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Tata", "Nexon", 2020, "Hubballi",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        // base = 8000, loading = 400, total = 8400
        assertEquals(new BigDecimal("8400.00"), response.getPremium());
        assertEquals(new BigDecimal("8000.00"), response.getBreakdown().getBasePremium());
        assertEquals(new BigDecimal("400.00"), response.getBreakdown().getAgeLoading());
    }

    @Test
    @DisplayName("Boundary: Exactly 10 years old receives 5% age loading")
    void testBoundary_VehicleAgeExactly10Years() {
        // In 2026, regYear 2016 gives age 10
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Honda", "City", 2016, "Dharwad",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        // base = 8000, loading = 400, total = 8400
        assertEquals(new BigDecimal("8400.00"), response.getPremium());
        assertEquals(new BigDecimal("400.00"), response.getBreakdown().getAgeLoading());
    }

    @Test
    @DisplayName("Boundary: Vehicle younger than 6 years (age 5) receives NO age loading")
    void testBoundary_VehicleAge5Years_NoLoading() {
        // In 2026, regYear 2021 gives age 5
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Toyota", "Glanza", 2021, "Mangaluru",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        assertEquals(new BigDecimal("8000.00"), response.getPremium());
        assertEquals(new BigDecimal("0.00"), response.getBreakdown().getAgeLoading());
    }

    @Test
    @DisplayName("Boundary: Vehicle older than 10 years (age 11) receives NO age loading")
    void testBoundary_VehicleAge11Years_NoLoading() {
        // In 2026, regYear 2015 gives age 11
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Maruti", "WagonR", 2015, "Mysuru",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        assertEquals(new BigDecimal("8000.00"), response.getPremium());
        assertEquals(new BigDecimal("0.00"), response.getBreakdown().getAgeLoading());
    }

    @Test
    @DisplayName("Clock advancement dynamically changes vehicle age and age loading")
    void testInjectedClockAdvancementChangesAge() {
        // Under 2026 clock, regYear 2021 is age 5 (loading 0)
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Tata", "Punch", 2021, "Belagavi",
                new BigDecimal("400000.00"), List.of()
        );

        QuoteResponse response2026 = quoteService.calculateMotorQuote(request, 1001L);
        assertEquals(new BigDecimal("0.00"), response2026.getBreakdown().getAgeLoading());

        // Advance clock by 1 year to 2027-10-19T00:00:00Z
        Clock clock2027 = Clock.fixed(Instant.parse("2027-10-19T00:00:00Z"), TEST_ZONE);
        MotorQuoteService quoteService2027 = new MotorQuoteService(
                quoteRepository, planQuery, ruleCache, clock2027, objectMapper
        );

        // In 2027, regYear 2021 is now age 6 -> receives 5% age loading!
        QuoteResponse response2027 = quoteService2027.calculateMotorQuote(request, 1001L);
        assertEquals(new BigDecimal("400.00"), response2027.getBreakdown().getAgeLoading());
        assertEquals(new BigDecimal("8400.00"), response2027.getPremium());
    }

    @Test
    @DisplayName("Anniversary boundary date: day before 6th anniversary has age 5; on anniversary has age 6")
    void testAnniversaryDateBoundary() {
        // Today is 2026-10-19.
        // Registered 2020-10-20 -> age 5 (anniversary tomorrow)
        int ageBeforeAnniversary = quoteService.calculateVehicleAge(null, LocalDate.of(2020, 10, 20));
        assertEquals(5, ageBeforeAnniversary);

        // Registered 2020-10-19 -> age 6 (anniversary today)
        int ageOnAnniversary = quoteService.calculateVehicleAge(null, LocalDate.of(2020, 10, 19));
        assertEquals(6, ageOnAnniversary);
    }

    @Test
    @DisplayName("Leap year anniversary boundary handling: Feb 29 registration")
    void testLeapYearAnniversaryBoundary() {
        // Leap day registration: 2020-02-29
        int ageIn2026 = quoteService.calculateVehicleAge(null, LocalDate.of(2020, 2, 29));
        assertEquals(6, ageIn2026);
    }

    @Test
    @DisplayName("Server ignores client-supplied premium override")
    void testClientSuppliedPremiumIsIgnored() {
        MotorQuoteRequest request = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2019, "Belagavi",
                new BigDecimal("400000.00"), List.of("ZERO_DEP")
        );
        // Client attempts to tamper with premium
        request.setPremium(new BigDecimal("100.00"));

        QuoteResponse response = quoteService.calculateMotorQuote(request, 1001L);

        // Must still calculate 9480.00 from authoritative rules
        assertEquals(new BigDecimal("9480.00"), response.getPremium());
    }

    @Test
    @DisplayName("Invalid IDV (negative or zero) throws ValidationException")
    void testInvalidIdv_ThrowsValidationException() {
        MotorQuoteRequest zeroIdv = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2020, "Belagavi",
                BigDecimal.ZERO, List.of()
        );
        assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(zeroIdv, 1001L));

        MotorQuoteRequest negativeIdv = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2020, "Belagavi",
                new BigDecimal("-50000.00"), List.of()
        );
        assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(negativeIdv, 1001L));
    }

    @Test
    @DisplayName("Future registration year throws ValidationException")
    void testFutureRegYear_ThrowsValidationException() {
        MotorQuoteRequest futureReg = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2030, "Belagavi",
                new BigDecimal("400000.00"), List.of()
        );
        ValidationException ex = assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(futureReg, 1001L));
        assertTrue(ex.getMessage().contains("future"));
    }

    @Test
    @DisplayName("Vehicle age exceeding maximum limit (20 years) throws ValidationException")
    void testVehicleAgeExceedingMax_ThrowsValidationException() {
        MotorQuoteRequest tooOld = new MotorQuoteRequest(
                1L, "4W", "Maruti", "800", 2000, "Belagavi", // In 2026, age is 26
                new BigDecimal("100000.00"), List.of()
        );
        ValidationException ex = assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(tooOld, 1001L));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowable limit"));
    }

    @Test
    @DisplayName("Unsupported add-on throws ValidationException")
    void testUnsupportedAddOn_ThrowsValidationException() {
        MotorQuoteRequest unsupportedAddOn = new MotorQuoteRequest(
                1L, "4W", "Maruti", "Swift", 2020, "Belagavi",
                new BigDecimal("400000.00"), List.of("HYDRO_LOCK_PROTECT")
        );
        ValidationException ex = assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(unsupportedAddOn, 1001L));
        assertTrue(ex.getMessage().contains("HYDRO_LOCK_PROTECT"));
    }

    @Test
    @DisplayName("Mismatched vehicle type (2W on 4W plan) throws ValidationException")
    void testMismatchedVehicleType_ThrowsValidationException() {
        MotorQuoteRequest mismatched = new MotorQuoteRequest(
                1L, "2W", "Honda", "Activa", 2020, "Belagavi",
                new BigDecimal("80000.00"), List.of()
        );
        ValidationException ex = assertThrows(ValidationException.class, () -> quoteService.calculateMotorQuote(mismatched, 1001L));
        assertTrue(ex.getMessage().contains("does not support two-wheelers"));
    }
}
