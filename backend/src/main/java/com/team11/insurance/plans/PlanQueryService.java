package com.team11.insurance.plans;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.common.exception.ResourceNotFoundException;

/**
 * Service implementing {@link IPlanQuery} for querying plans and rating rules from the database.
 */
@Service
@Transactional(readOnly = true)
public class PlanQueryService implements IPlanQuery {

    private static final Logger log = LoggerFactory.getLogger(PlanQueryService.class);
    private final PlanRepository planRepository;

    public PlanQueryService(PlanRepository planRepository) {
        this.planRepository = Objects.requireNonNull(planRepository, "planRepository must not be null");
    }

    @Override
    public Plan getPlan(long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
    }

    @Override
    public RatingRules getRatingRules(long planId) {
        Plan plan = getPlan(planId);
        String ruleType = plan.getType().startsWith("MOTOR") ? "R-M" : "R-H";
        return new RatingRules(
                plan.getId(),
                plan.getCode(),
                ruleType,
                plan.getPremiumRulesJson(),
                plan.getUnderwritingRulesJson()
        );
    }

    @Override
    public List<Plan> getActivePlans() {
        return planRepository.findByActiveTrue();
    }

    @Override
    public List<Plan> getPlansByInsurer(long insurerId) {
        return planRepository.findByInsurerIdAndActiveTrue(insurerId);
    }

    @Override
    public List<Plan> getPlansByType(String type) {
        return planRepository.findByTypeAndActiveTrue(type);
    }

    @Override
    public boolean existsById(long planId) {
        return planRepository.existsById(planId);
    }
}
