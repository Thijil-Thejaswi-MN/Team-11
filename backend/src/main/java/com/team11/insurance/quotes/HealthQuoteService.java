package com.team11.insurance.quotes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.RatingRules;
import com.team11.insurance.quotes.dto.HealthMemberRequest;
import com.team11.insurance.quotes.dto.HealthQuoteRequest;
import com.team11.insurance.quotes.dto.PremiumBreakdown;
import com.team11.insurance.quotes.dto.QuoteResponse;

/**
 * Service for calculating health insurance quotes (INS-F-006, P1-18, TC-Quote-02, SAD §4.3.3).
 * Evaluates individual member base premiums according to age bands, applies sum-insured
 * and pre-existing disease rating multipliers, and persists calculated quotes.
 */
@Service
public class HealthQuoteService {

    private static final Logger log = LoggerFactory.getLogger(HealthQuoteService.class);

    public static final int MIN_MEMBER_AGE = 18;
    public static final int MAX_MEMBER_AGE = 120;
    public static final Duration QUOTE_VALIDITY_DURATION = Duration.ofDays(30);

    private final IPlanQuery planQuery;
    private final QuoteRepository quoteRepository;
    private final PlanRatingRuleCache ruleCache;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public HealthQuoteService(IPlanQuery planQuery,
                              QuoteRepository quoteRepository,
                              PlanRatingRuleCache ruleCache,
                              ObjectMapper objectMapper,
                              ObjectProvider<Clock> clockProvider) {
        this(planQuery, quoteRepository, ruleCache, objectMapper,
                clockProvider.getIfAvailable(Clock::systemUTC));
    }

    public HealthQuoteService(IPlanQuery planQuery,
                              QuoteRepository quoteRepository,
                              PlanRatingRuleCache ruleCache,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.planQuery = Objects.requireNonNull(planQuery, "planQuery must not be null");
        this.quoteRepository = Objects.requireNonNull(quoteRepository, "quoteRepository must not be null");
        this.ruleCache = Objects.requireNonNull(ruleCache, "ruleCache must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Calculates a health insurance quote for single or multiple members.
     * Enforces input validations, plan compatibility, sum insured support, and rating rules.
     *
     * @param request Quote calculation parameters
     * @param customerId ID of the authenticated customer
     * @return QuoteResponse with quoteId, premium, breakdown, and validity timestamp
     */
    @Transactional
    public QuoteResponse calculateHealthQuote(HealthQuoteRequest request, Long customerId) {
        validateRequest(request);

        Plan plan = planQuery.getPlan(request.getPlanId());
        validatePlanCompatibility(plan);

        // Cache lookup for plan rating rules
        HealthRatingRules ratingRules = ruleCache.getOrLoadHealth(plan.getId(), id -> loadAndParseRatingRules(plan));

        // 1. Calculate sum of individual member base premiums
        BigDecimal basePremiumTotal = BigDecimal.ZERO;
        List<Map<String, Object>> membersDetails = new ArrayList<>();

        for (HealthMemberRequest member : request.getMembers()) {
            validateMember(member);
            BigDecimal memberBase = ratingRules.calculateMemberBase(member.getAge());
            basePremiumTotal = basePremiumTotal.add(memberBase);

            Map<String, Object> memberMap = new LinkedHashMap<>();
            memberMap.put("age", member.getAge());
            if (member.getGender() != null) {
                memberMap.put("gender", member.getGender());
            }
            if (member.getRelation() != null) {
                memberMap.put("relation", member.getRelation());
            }
            if (member.getPreExisting() != null) {
                memberMap.put("preExisting", member.getPreExisting());
            }
            memberMap.put("basePremium", memberBase);
            membersDetails.add(memberMap);
        }

        // 2. Lookup sum insured multiplier (validates sum insured is offered by plan)
        BigDecimal sumInsuredMultiplier = ratingRules.getSumInsuredMultiplier(request.getSumInsured());

        // 3. Lookup pre-existing disease multiplier (supports request-level or any member-level declaration)
        boolean hasMemberPreExisting = request.getMembers().stream().anyMatch(HealthMemberRequest::isPreExisting);
        boolean hasPreExisting = request.isPreExisting() || hasMemberPreExisting;
        BigDecimal preExistingMultiplier = hasPreExisting ? ratingRules.getPreExistingMultiplier() : BigDecimal.ONE;

        // 4. Calculate total premium: basePremiumTotal * sumInsuredMultiplier * preExistingMultiplier
        BigDecimal premiumAfterSumInsured = basePremiumTotal.multiply(sumInsuredMultiplier);
        BigDecimal totalPremium = premiumAfterSumInsured.multiply(preExistingMultiplier).setScale(2, RoundingMode.HALF_UP);

        // 5. Build breakdown
        BigDecimal loadingAmount = totalPremium.subtract(basePremiumTotal).setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> factorBreakdown = new LinkedHashMap<>();
        factorBreakdown.put("SUM_INSURED_FACTOR", sumInsuredMultiplier);
        if (hasPreExisting) {
            factorBreakdown.put("PRE_EXISTING_FACTOR", preExistingMultiplier);
        }

        PremiumBreakdown breakdown = new PremiumBreakdown(
                basePremiumTotal.setScale(2, RoundingMode.HALF_UP),
                loadingAmount,
                factorBreakdown,
                totalPremium
        );

        Instant validUntil = Instant.now(clock).plus(QUOTE_VALIDITY_DURATION);

        // 6. Serialize details and breakdown for audit and Quote persistence
        String breakdownJson = serializeJson(breakdown);
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        detailsMap.put("planId", plan.getId());
        detailsMap.put("planCode", plan.getCode());
        detailsMap.put("productType", "HEALTH");
        detailsMap.put("sumInsured", request.getSumInsured());
        detailsMap.put("preExisting", hasPreExisting);
        detailsMap.put("memberCount", request.getMembers().size());
        detailsMap.put("members", membersDetails);
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

        log.info("Health quote generated: quoteId={}, plan={}, members={}, sumInsured={}, preExisting={}, totalPremium={}",
                savedQuote.getId(), plan.getCode(), request.getMembers().size(), request.getSumInsured(), hasPreExisting, totalPremium);

        return new QuoteResponse(savedQuote.getId(), totalPremium, breakdown, validUntil);
    }

    private void validateRequest(HealthQuoteRequest request) {
        if (request == null) {
            throw new ValidationException("Quote request body is missing.");
        }
        if (request.getPlanId() == null) {
            throw new ValidationException("Field 'planId' is required.");
        }
        if (request.getMembers() == null || request.getMembers().isEmpty()) {
            throw new ValidationException("Field 'members' must contain at least one member.");
        }
        if (request.getSumInsured() == null || request.getSumInsured().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Field 'sumInsured' must be greater than zero.");
        }
    }

    private void validateMember(HealthMemberRequest member) {
        if (member == null) {
            throw new ValidationException("Member record cannot be null.");
        }
        if (member.getAge() == null) {
            throw new ValidationException("Member age is required.");
        }
        if (member.getAge() < MIN_MEMBER_AGE) {
            throw new ValidationException("Member age " + member.getAge() + " is below minimum allowable age of " + MIN_MEMBER_AGE + " years.");
        }
        if (member.getAge() > MAX_MEMBER_AGE) {
            throw new ValidationException("Member age " + member.getAge() + " exceeds maximum allowable limit of " + MAX_MEMBER_AGE + " years.");
        }
    }

    private void validatePlanCompatibility(Plan plan) {
        if (!plan.isActive()) {
            throw new ValidationException("Selected plan '" + plan.getCode() + "' is inactive.");
        }

        String planType = plan.getType();
        if (!"HEALTH".equalsIgnoreCase(planType)) {
            throw new ValidationException("Plan '" + plan.getCode() + "' (" + planType + ") is not a health insurance plan.");
        }
    }

    private HealthRatingRules loadAndParseRatingRules(Plan plan) {
        RatingRules rules = planQuery.getRatingRules(plan.getId());
        String json = rules.getPremiumRulesJson();
        if (json == null || json.isBlank()) {
            log.warn("Plan {} has no premiumRulesJson configured, using default R-H rating rules.", plan.getCode());
            return new HealthRatingRules();
        }
        try {
            return objectMapper.readValue(json, HealthRatingRules.class);
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
