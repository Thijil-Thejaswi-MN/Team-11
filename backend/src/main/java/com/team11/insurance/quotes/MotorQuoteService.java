package com.team11.insurance.quotes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.RatingRules;
import com.team11.insurance.quotes.dto.MotorQuoteRequest;
import com.team11.insurance.quotes.dto.PremiumBreakdown;
import com.team11.insurance.quotes.dto.QuoteResponse;

/**
 * Service implementing the Motor Quote Engine (C5, INS-F-005, P1-06).
 * Calculates server-side premiums using plan rating rules and injected Clock.
 */
@Service
public class MotorQuoteService {

    private static final Logger log = LoggerFactory.getLogger(MotorQuoteService.class);
    private static final int MAX_VEHICLE_AGE_YEARS = 20;

    private final QuoteRepository quoteRepository;
    private final IPlanQuery planQuery;
    private final PlanRatingRuleCache ruleCache;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public MotorQuoteService(QuoteRepository quoteRepository,
                             IPlanQuery planQuery,
                             PlanRatingRuleCache ruleCache,
                             Clock clock,
                             ObjectMapper objectMapper) {
        this.quoteRepository = Objects.requireNonNull(quoteRepository, "quoteRepository must not be null");
        this.planQuery = Objects.requireNonNull(planQuery, "planQuery must not be null");
        this.ruleCache = Objects.requireNonNull(ruleCache, "ruleCache must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Calculates a motor insurance quote according to the plan's rating rule.
     * Guaranteed server-side calculation: client-supplied premiums are strictly ignored.
     *
     * @param request validated motor quote request payload
     * @param customerId ID of the authenticated customer
     * @return QuoteResponse with quoteId, premium, and detailed breakdown
     */
    @Transactional
    public QuoteResponse calculateMotorQuote(MotorQuoteRequest request, Long customerId) {
        validateRequest(request);

        Plan plan = planQuery.getPlan(request.getPlanId());
        validatePlanCompatibility(plan, request.getVehicleType());

        int vehicleAge = calculateVehicleAge(request.getRegYear(), request.getRegDate());
        if (vehicleAge > MAX_VEHICLE_AGE_YEARS) {
            throw new ValidationException("Vehicle age of " + vehicleAge + " years exceeds the maximum allowable limit of " + MAX_VEHICLE_AGE_YEARS + " years.");
        }

        // Cache lookup for plan rating rules
        MotorRatingRules ratingRules = ruleCache.getOrLoad(plan.getId(), id -> loadAndParseRatingRules(plan));

        // 1. Base premium: IDV * ownDamageBasePct (default 2%)
        BigDecimal basePremium = ratingRules.calculateBasePremium(request.getIdv());

        // 2. Age loading: 5% of base for vehicle age 6 to 10 years inclusive
        BigDecimal ageLoading = ratingRules.calculateAgeLoading(basePremium, vehicleAge);

        // 3. Add-ons: lookup flat fee per add-on
        Map<String, BigDecimal> addOnBreakdown = new LinkedHashMap<>();
        BigDecimal addOnsTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.getAddOns() != null) {
            for (String addOn : request.getAddOns()) {
                String normalizedAddOn = addOn.trim().toUpperCase();
                if (!ratingRules.supportsAddOn(normalizedAddOn)) {
                    throw new ValidationException("Add-on '" + addOn + "' is not supported by plan " + plan.getCode() + ".");
                }
                BigDecimal price = ratingRules.getAddOnPrice(normalizedAddOn);
                addOnBreakdown.put(normalizedAddOn, price);
                addOnsTotal = addOnsTotal.add(price);
            }
        }

        // 4. Total premium = basePremium + ageLoading + addOns
        BigDecimal totalPremium = basePremium.add(ageLoading).add(addOnsTotal).setScale(2, RoundingMode.HALF_UP);

        PremiumBreakdown breakdown = new PremiumBreakdown(basePremium, ageLoading, addOnBreakdown, totalPremium);

        Instant validUntil = Instant.now(clock).plus(Duration.ofDays(30));

        // Persist Quote entity
        String breakdownJson = serializeJson(breakdown);
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        detailsMap.put("planId", plan.getId());
        detailsMap.put("planCode", plan.getCode());
        detailsMap.put("vehicleType", request.getVehicleType());
        detailsMap.put("make", request.getMake());
        detailsMap.put("model", request.getModel());
        detailsMap.put("regYear", request.getRegYear());
        detailsMap.put("calculatedVehicleAge", vehicleAge);
        detailsMap.put("city", request.getCity());
        detailsMap.put("idv", request.getIdv());
        detailsMap.put("addOns", request.getAddOns());
        String detailsJson = serializeJson(detailsMap);

        Quote quote = new Quote(
                customerId != null ? customerId : 1001L,
                plan.getInsurerId(),
                plan.getId(),
                plan.getType(),
                totalPremium,
                breakdownJson,
                detailsJson,
                validUntil
        );

        Quote savedQuote = quoteRepository.save(quote);

        log.info("Motor quote generated: quoteId={}, plan={}, vehicleAge={}, IDV={}, totalPremium={}",
                savedQuote.getId(), plan.getCode(), vehicleAge, request.getIdv(), totalPremium);

        return new QuoteResponse(savedQuote.getId(), totalPremium, breakdown, validUntil);
    }

    /**
     * Calculates the vehicle age in full years using the injected Clock.
     */
    public int calculateVehicleAge(Integer regYear, LocalDate regDate) {
        LocalDate today = LocalDate.now(clock);
        if (regDate != null) {
            if (regDate.isAfter(today)) {
                throw new ValidationException("Vehicle registration date (" + regDate + ") cannot be in the future.");
            }
            return Period.between(regDate, today).getYears();
        }

        int currentYear = today.getYear();
        if (regYear > currentYear) {
            throw new ValidationException("Vehicle registration year (" + regYear + ") cannot be in the future.");
        }
        if (regYear < 1900) {
            throw new ValidationException("Vehicle registration year (" + regYear + ") is invalid.");
        }
        return currentYear - regYear;
    }

    private void validateRequest(MotorQuoteRequest request) {
        if (request == null) {
            throw new ValidationException("Quote request body is missing.");
        }
        if (request.getPlanId() == null) {
            throw new ValidationException("Field 'planId' is required.");
        }
        if (request.getVehicleType() == null || request.getVehicleType().isBlank()) {
            throw new ValidationException("Field 'vehicleType' is required.");
        }
        if (request.getMake() == null || request.getMake().isBlank()) {
            throw new ValidationException("Field 'make' is required.");
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new ValidationException("Field 'model' is required.");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new ValidationException("Field 'city' is required.");
        }
        if (request.getIdv() == null || request.getIdv().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Field 'idv' must be greater than zero.");
        }
        if (request.getRegYear() == null && request.getRegDate() == null) {
            throw new ValidationException("Field 'regYear' is required.");
        }
    }

    private void validatePlanCompatibility(Plan plan, String vehicleType) {
        if (!plan.isActive()) {
            throw new ValidationException("Selected plan '" + plan.getCode() + "' is inactive.");
        }

        String normalizedType = vehicleType.trim().toUpperCase();
        boolean is4W = "4W".equals(normalizedType) || "MOTOR_FOUR_WHEELER".equals(normalizedType);
        boolean is2W = "2W".equals(normalizedType) || "MOTOR_TWO_WHEELER".equals(normalizedType);

        if (!is4W && !is2W) {
            throw new ValidationException("Unsupported vehicle type '" + vehicleType + "'. Allowed types: 2W, 4W.");
        }

        String planType = plan.getType();
        if (is4W && !"MOTOR_FOUR_WHEELER".equalsIgnoreCase(planType)) {
            throw new ValidationException("Plan '" + plan.getCode() + "' (" + planType + ") does not support four-wheelers.");
        }
        if (is2W && !"MOTOR_TWO_WHEELER".equalsIgnoreCase(planType)) {
            throw new ValidationException("Plan '" + plan.getCode() + "' (" + planType + ") does not support two-wheelers.");
        }
    }

    private MotorRatingRules loadAndParseRatingRules(Plan plan) {
        RatingRules rules = planQuery.getRatingRules(plan.getId());
        String json = rules.getPremiumRulesJson();
        if (json == null || json.isBlank()) {
            log.warn("Plan {} has no premiumRulesJson configured, using default R-M rating rules.", plan.getCode());
            return new MotorRatingRules();
        }
        try {
            return objectMapper.readValue(json, MotorRatingRules.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse rating rules JSON for plan {}: {}", plan.getCode(), json, e);
            throw new ValidationException("Invalid rating rule configuration for plan " + plan.getCode());
        }
    }

    private String serializeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", object, e);
            return "{}";
        }
    }
}
