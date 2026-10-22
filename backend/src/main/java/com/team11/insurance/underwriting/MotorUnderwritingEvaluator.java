package com.team11.insurance.underwriting;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.MotorRules;

/**
 * Evaluates motor insurance applications against vehicle-age and active-policy rules (SAD §4.3.7, SRS §4.4.1).
 * <p>
 * Evaluation rules and precedence:
 * <ul>
 *   <li>Vehicle age &gt; 20 years &rarr; {@code REJECT}</li>
 *   <li>Vehicle age &gt; 15 years and &le; 20 years &rarr; {@code REFER}</li>
 *   <li>Vehicle age &le; 15 years &rarr; {@code APPROVE} (unless another rule rejects or refers)</li>
 *   <li>Active policy exists on the vehicle &rarr; {@code REJECT} (overrides age-based Approve or Refer)</li>
 * </ul>
 * The strictest outcome wins: {@code APPROVE &lt; REFER &lt; REJECT}.
 */
@Component
public class MotorUnderwritingEvaluator {

    private static final Logger log = LoggerFactory.getLogger(MotorUnderwritingEvaluator.class);

    private final IPolicyQuery policyQuery;
    private final ObjectMapper objectMapper;

    public MotorUnderwritingEvaluator(IPolicyQuery policyQuery, ObjectMapper objectMapper) {
        this.policyQuery = Objects.requireNonNull(policyQuery, "policyQuery must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public UnderwritingResult evaluate(Application application, MotorRules rules, Clock clock) {
        if (rules == null || !rules.isValid()) {
            return UnderwritingResult.refer("Missing rule configuration");
        }

        Clock effectiveClock = clock != null ? clock : Clock.systemDefaultZone();
        int currentYear = LocalDate.now(effectiveClock).getYear();

        // 1. Extract vehicle registration number and check for duplicate active policy
        // Mandatory business rule (SAD §4.3.7): duplicate active policy must ALWAYS result in REJECT.
        // Plan configuration cannot disable this rule, and it takes precedence even if vehicle age is missing.
        String vehicleRegNo = extractVehicleRegNo(application);
        boolean activePolicyExists = false;
        if (vehicleRegNo != null && !vehicleRegNo.isBlank()) {
            activePolicyExists = policyQuery.existsActivePolicyForVehicle(vehicleRegNo);
            if (activePolicyExists) {
                log.info("MotorUnderwriting: Active policy detected for vehicle reg '{}' on application {}",
                        vehicleRegNo, application != null ? application.getId() : null);
                String rejectReason = "Active policy already exists for vehicle: " + vehicleRegNo;
                return UnderwritingResult.reject(rejectReason);
            }
        }

        // 2. Extract vehicle age
        Integer vehicleAge = extractVehicleAge(application, currentYear);
        if (vehicleAge == null) {
            log.warn("MotorUnderwriting: Unable to determine vehicle age for application {}",
                    application != null ? application.getId() : null);
            return UnderwritingResult.refer("Missing vehicle registration year or age in application details");
        }

        // 3. Validate against negative vehicle age or future registration year
        if (vehicleAge < 0) {
            throw new com.team11.insurance.common.exception.ValidationException(
                    "Invalid vehicle age: registration year cannot be in the future (calculated age: " + vehicleAge + ")");
        }

        // 4. Evaluate vehicle age boundary rules
        if (vehicleAge > rules.getMaxVehicleAgeYears()) {
            return UnderwritingResult.reject("Vehicle age exceeds maximum threshold: "
                    + vehicleAge + " years (max allowed " + rules.getMaxVehicleAgeYears() + ")");
        } else if (vehicleAge > rules.getReferralVehicleAgeYears()) {
            return UnderwritingResult.refer("Vehicle age requires referral: "
                    + vehicleAge + " years (referral threshold " + rules.getReferralVehicleAgeYears() + ")");
        } else {
            return UnderwritingResult.approve("Vehicle age within acceptable limit: " + vehicleAge + " years");
        }
    }

    private String extractVehicleRegNo(Application application) {
        if (application == null) {
            return null;
        }
        if (application.getVehicleRegNo() != null && !application.getVehicleRegNo().isBlank()) {
            return application.getVehicleRegNo().trim();
        }
        if (application.getDetailsJson() != null && !application.getDetailsJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(application.getDetailsJson());
                if (root.hasNonNull("vehicleRegNo")) {
                    return root.get("vehicleRegNo").asText().trim();
                }
            } catch (Exception e) {
                log.debug("Failed to parse vehicleRegNo from detailsJson: {}", e.getMessage());
            }
        }
        return null;
    }

    private Integer extractVehicleAge(Application application, int currentYear) {
        if (application == null || application.getDetailsJson() == null || application.getDetailsJson().isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(application.getDetailsJson());
            if (root.hasNonNull("regYear")) {
                int regYear = root.get("regYear").asInt();
                return currentYear - regYear;
            }
            if (root.hasNonNull("vehicleAge")) {
                return root.get("vehicleAge").asInt();
            }
            if (root.hasNonNull("age")) {
                return root.get("age").asInt();
            }
        } catch (Exception e) {
            log.warn("Error parsing vehicle age from detailsJson: {}", e.getMessage());
        }
        return null;
    }
}
