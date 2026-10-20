package com.team11.insurance.underwriting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.HealthRules;

/**
 * Evaluates health insurance applications based on oldest member age and pre-existing conditions (SAD §4.3.7, SRS §4.4.1).
 * <p>
 * Evaluation rules and precedence:
 * <ul>
 *   <li>Oldest member's age &gt; 70 &rarr; {@code REJECT}</li>
 *   <li>Oldest member's age 60–70 inclusive &rarr; {@code REFER}</li>
 *   <li>Pre-existing disease is present &rarr; {@code REFER} unless age rule yields Reject</li>
 *   <li>Age below 60 with no pre-existing disease &rarr; {@code APPROVE}</li>
 * </ul>
 * The strictest outcome wins: {@code APPROVE &lt; REFER &lt; REJECT}.
 */
@Component
public class HealthUnderwritingEvaluator {

    private static final Logger log = LoggerFactory.getLogger(HealthUnderwritingEvaluator.class);

    private final ObjectMapper objectMapper;

    public HealthUnderwritingEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public UnderwritingResult evaluate(Application application, HealthRules rules) {
        if (rules == null || !rules.isValid()) {
            return UnderwritingResult.refer("Missing rule configuration");
        }

        // 1. Extract member ages and pre-existing condition flags
        HealthProfile profile = extractHealthProfile(application);
        if (profile.oldestAge == null) {
            log.warn("HealthUnderwriting: No applicant or member age provided for application {}",
                    application != null ? application.getId() : null);
            return UnderwritingResult.refer("Missing applicant or member age in application details");
        }

        int oldestAge = profile.oldestAge;
        boolean hasPreExisting = profile.hasPreExisting;

        // 2. Evaluate age rule
        UnderwritingResult ageResult;
        if (oldestAge > rules.getMaxAge()) {
            ageResult = UnderwritingResult.reject("Oldest member age exceeds maximum threshold: "
                    + oldestAge + " years (max allowed " + rules.getMaxAge() + ")");
        } else if (oldestAge >= rules.getReferralAge()) {
            ageResult = UnderwritingResult.refer("Oldest member age requires referral: "
                    + oldestAge + " years (referral threshold " + rules.getReferralAge() + "-" + rules.getMaxAge() + ")");
        } else {
            ageResult = UnderwritingResult.approve("Oldest member age within acceptable limits: " + oldestAge + " years");
        }

        // 3. Evaluate pre-existing disease rule
        UnderwritingResult preExistingResult = null;
        if (hasPreExisting && rules.getPreExistingReferral()) {
            preExistingResult = UnderwritingResult.refer("Pre-existing disease declared requires referral");
        }

        // 4. Strictest outcome precedence: APPROVE < REFER < REJECT
        if (ageResult.isRejected()) {
            // Reject is stricter than any referral for pre-existing disease
            return ageResult;
        }

        if (preExistingResult != null && preExistingResult.isReferred()) {
            if (ageResult.isReferred()) {
                // Both age and pre-existing condition yield REFER; provide combined clarity
                return UnderwritingResult.refer(ageResult.getReason() + "; " + preExistingResult.getReason());
            }
            // Age was APPROVE, but pre-existing yields REFER -> REFER wins
            return preExistingResult;
        }

        return ageResult;
    }

    private HealthProfile extractHealthProfile(Application application) {
        if (application == null || application.getDetailsJson() == null || application.getDetailsJson().isBlank()) {
            return new HealthProfile(null, false);
        }

        try {
            JsonNode root = objectMapper.readTree(application.getDetailsJson());
            List<Integer> ages = new ArrayList<>();
            boolean preExisting = false;

            // Check top-level pre-existing condition declarations
            if (root.has("preExisting")) {
                JsonNode pe = root.get("preExisting");
                if (pe.isBoolean()) {
                    preExisting = pe.asBoolean();
                } else if (pe.isTextual()) {
                    preExisting = "true".equalsIgnoreCase(pe.asText()) || "yes".equalsIgnoreCase(pe.asText());
                }
            } else if (root.has("hasPreExisting")) {
                JsonNode pe = root.get("hasPreExisting");
                if (pe.isBoolean()) {
                    preExisting = pe.asBoolean();
                } else if (pe.isTextual()) {
                    preExisting = "true".equalsIgnoreCase(pe.asText()) || "yes".equalsIgnoreCase(pe.asText());
                }
            }

            // Check top-level applicant age
            if (root.hasNonNull("applicantAge")) {
                ages.add(root.get("applicantAge").asInt());
            } else if (root.hasNonNull("age")) {
                ages.add(root.get("age").asInt());
            }

            // Check members array
            if (root.has("members") && root.get("members").isArray()) {
                for (JsonNode member : root.get("members")) {
                    if (member.hasNonNull("age")) {
                        ages.add(member.get("age").asInt());
                    } else if (member.hasNonNull("memberAge")) {
                        ages.add(member.get("memberAge").asInt());
                    }
                    if (member.has("preExisting")) {
                        JsonNode mpe = member.get("preExisting");
                        if (mpe.isBoolean() && mpe.asBoolean()) {
                            preExisting = true;
                        } else if (mpe.isTextual() && ("true".equalsIgnoreCase(mpe.asText()) || "yes".equalsIgnoreCase(mpe.asText()))) {
                            preExisting = true;
                        }
                    }
                }
            }

            Integer oldestAge = ages.stream().max(Integer::compareTo).orElse(null);
            return new HealthProfile(oldestAge, preExisting);
        } catch (Exception e) {
            log.warn("Failed to extract health profile from detailsJson: {}", e.getMessage());
            return new HealthProfile(null, false);
        }
    }

    private static record HealthProfile(Integer oldestAge, boolean hasPreExisting) {}
}
