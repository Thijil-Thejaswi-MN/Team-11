package com.team11.insurance.underwriting;

import java.util.Objects;

/**
 * Result object returned by IUnderwriting.evaluate(application).
 * Encapsulates the evaluation outcome and reason (SAD 4.3.7, PR #53).
 */
public class UnderwritingResult {

    private final UnderwritingOutcome outcome;
    private final String reason;

    public UnderwritingResult(UnderwritingOutcome outcome, String reason) {
        this.outcome = Objects.requireNonNull(outcome, "Underwriting outcome must not be null");
        this.reason = reason;
    }

    public static UnderwritingResult approve(String reason) {
        return new UnderwritingResult(UnderwritingOutcome.APPROVE, reason);
    }

    public static UnderwritingResult refer(String reason) {
        return new UnderwritingResult(UnderwritingOutcome.REFER, reason);
    }

    public static UnderwritingResult reject(String reason) {
        return new UnderwritingResult(UnderwritingOutcome.REJECT, reason);
    }

    public UnderwritingOutcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public boolean isApproved() {
        return outcome == UnderwritingOutcome.APPROVE;
    }

    public boolean isReferred() {
        return outcome == UnderwritingOutcome.REFER;
    }

    public boolean isRejected() {
        return outcome == UnderwritingOutcome.REJECT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnderwritingResult that)) return false;
        return outcome == that.outcome && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outcome, reason);
    }

    @Override
    public String toString() {
        return "UnderwritingResult{" +
                "outcome=" + outcome +
                ", reason='" + reason + '\'' +
                '}';
    }
}
