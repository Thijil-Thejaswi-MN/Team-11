package com.team11.insurance.underwriting;

import com.team11.insurance.applications.Application;

/**
 * Internal interface for the Underwriting Engine (C6) - SAD Section 4.3.7.
 * Consumers: Quote & Purchase Module (C5).
 */
public interface IUnderwriting {

    /**
     * Evaluates an application against data-driven rules (SRS 4.4.1).
     *
     * @param application the submitted application to evaluate
     * @return the evaluation outcome (APPROVE, REFER, or REJECT) with a reason
     */
    UnderwritingResult evaluate(Application application);

    /**
     * Records a manual decision made by an Underwriter on a referred application (INS-F-011).
     *
     * @param applicationId the ID of the referred application
     * @param decision the manual decision (APPROVE or REJECT)
     * @param reason the justification provided by the underwriter
     * @param underwriterId the user ID of the acting underwriter
     */
    void recordManualDecision(long applicationId, Decision decision, String reason, long underwriterId);
}
