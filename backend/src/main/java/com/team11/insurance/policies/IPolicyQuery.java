package com.team11.insurance.policies;

/** Provided by Quote & Purchase (C5). Consumers: Claims Workflow, Underwriting Engine. */
public interface IPolicyQuery {

    /** @throws com.team11.insurance.common.exception.ResourceNotFoundException if there is no such policy */
    PolicyView getPolicy(long policyId);

    /**
     * PROPOSED (contract section 4): true if any policy with this registration number is ACTIVE or
     * CANCELLATION_REQUESTED and its end date is not in the past. Checks across all insurers.
     */
    boolean existsActivePolicyForVehicle(String vehicleRegNo);
}
