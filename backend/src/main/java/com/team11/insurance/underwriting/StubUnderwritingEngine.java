package com.team11.insurance.underwriting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.team11.insurance.applications.Application;

/**
 * Temporary development and test-only placeholder implementation of {@link IUnderwriting} (P1-05).
 * <p>
 * <strong>IMPORTANT:</strong> This stub always returns {@code APPROVE} with reason
 * {@code TEST_STUB_ALWAYS_APPROVES}. It exists solely for early integration testing
 * (allowing P3/P4 application workflows to execute) and must not be mistaken for the
 * real data-driven underwriting rules engine.
 * <p>
 * Restricted to {@code dev} and {@code test} profiles. It must never be registered in
 * production; under the {@code prod} profile, no stub bean is loaded, ensuring that the
 * system fails closed if the real underwriting engine is not configured.
 */
@Service
@Profile({"dev", "test"})
public class StubUnderwritingEngine implements IUnderwriting {

    private static final Logger log = LoggerFactory.getLogger(StubUnderwritingEngine.class);
    public static final String STUB_REASON = "TEST_STUB_ALWAYS_APPROVES";

    @Override
    public UnderwritingResult evaluate(Application application) {
        log.warn("StubUnderwritingEngine evaluated application ID {} - ALWAYS APPROVE STUB ACTIVE",
                application != null ? application.getId() : null);
        return UnderwritingResult.approve(STUB_REASON);
    }

    @Override
    public void recordManualDecision(long applicationId, Decision decision, String reason, long underwriterId) {
        log.info("StubUnderwritingEngine: Recorded manual decision for application {}: {} by underwriter {}. Reason: {}",
                applicationId, decision, underwriterId, reason);
    }
}
