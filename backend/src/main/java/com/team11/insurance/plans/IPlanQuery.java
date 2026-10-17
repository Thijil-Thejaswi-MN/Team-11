package com.team11.insurance.plans;

import java.util.List;

/**
 * Internal interface for querying plans and rating rules across modules
 * (Component C4 - SAD 3.4 & 4.3.7, API Contracts 01 & 02).
 * <p>
 * Consumed by Quote Engine (C5), Underwriting (C6), and Application (C7).
 */
public interface IPlanQuery {

    /**
     * Retrieves a plan by its primary identifier.
     *
     * @param planId the ID of the plan
     * @return the Plan entity
     * @throws com.team11.insurance.common.exception.ResourceNotFoundException if no plan exists with the given ID
     */
    Plan getPlan(long planId);

    /**
     * Retrieves the rating rules (premium and underwriting) for a given plan.
     *
     * @param planId the ID of the plan
     * @return the RatingRules associated with the plan
     * @throws com.team11.insurance.common.exception.ResourceNotFoundException if no plan exists with the given ID
     */
    RatingRules getRatingRules(long planId);

    /**
     * Retrieves all active plans available in the system.
     *
     * @return list of active plans
     */
    List<Plan> getActivePlans();

    /**
     * Retrieves active plans for a specific insurer.
     *
     * @param insurerId the insurer ID
     * @return list of active plans for the insurer
     */
    List<Plan> getPlansByInsurer(long insurerId);

    /**
     * Retrieves active plans for a specific product type (e.g., MOTOR_FOUR_WHEELER, HEALTH).
     *
     * @param type the plan type
     * @return list of active plans of that type
     */
    List<Plan> getPlansByType(String type);

    /**
     * Checks if a plan exists with the given identifier.
     *
     * @param planId the plan ID
     * @return true if the plan exists, false otherwise
     */
    boolean existsById(long planId);
}
