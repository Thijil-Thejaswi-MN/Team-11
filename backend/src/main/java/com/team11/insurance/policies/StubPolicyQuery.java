package com.team11.insurance.policies;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.team11.insurance.common.exception.ResourceNotFoundException;

/**
 * In-memory stand-in so other modules can build against IPolicyQuery before real policies exist.
 * Only active under the dev/test profiles. DELETE IT when the real database-backed
 * implementation is added, otherwise two IPolicyQuery beans exist in the dev profile.
 * Uses a Clock bean if one exists (test-clock rule), else the system clock.
 */
@Component
@Profile({"dev", "test"})
public class StubPolicyQuery implements IPolicyQuery {

    private final Map<Long, PolicyView> policies = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public StubPolicyQuery(ObjectProvider<Clock> clockProvider) {
        this(clockProvider.getIfAvailable(Clock::systemDefaultZone));
    }

    StubPolicyQuery(Clock clock) {
        this.clock = clock;
    }

    /** Dev/test helper: seed a policy. */
    public void add(PolicyView policy) {
        policies.put(policy.id(), policy);
    }

    @Override
    public PolicyView getPolicy(long policyId) {
        PolicyView policy = policies.get(policyId);
        if (policy == null) {
            throw new ResourceNotFoundException("Policy", policyId);
        }
        return policy;
    }

    @Override
    public boolean existsActivePolicyForVehicle(String vehicleRegNo) {
        if (vehicleRegNo == null || vehicleRegNo.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(clock);
        return policies.values().stream().anyMatch(p ->
                vehicleRegNo.equalsIgnoreCase(p.vehicleRegNo())
                        && (p.status() == PolicyStatus.ACTIVE
                            || p.status() == PolicyStatus.CANCELLATION_REQUESTED)
                        && !p.endDate().isBefore(today));
    }
}
