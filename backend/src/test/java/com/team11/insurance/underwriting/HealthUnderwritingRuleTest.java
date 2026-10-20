package com.team11.insurance.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.underwriting.UnderwritingRuleConfig.HealthRules;

/**
 * Unit test suite for health underwriting boundary rules and precedence (TC-UW-02, INS-F-010).
 * <p>
 * Verifies exact acceptance criteria:
 * <ul>
 *   <li>Oldest member age 45, no pre-existing &rarr; Approve</li>
 *   <li>Oldest member age 59, no pre-existing &rarr; Approve (boundary: &lt; 60)</li>
 *   <li>Oldest member age 60, no pre-existing &rarr; Refer (boundary: 60–70 inclusive)</li>
 *   <li>Oldest member age 70, no pre-existing &rarr; Refer (boundary: 60–70 inclusive)</li>
 *   <li>Oldest member age 71, no pre-existing &rarr; Reject (boundary: &gt; 70)</li>
 *   <li>Age 30, pre-existing disease Yes &rarr; Refer</li>
 *   <li>Age 72, pre-existing disease Yes &rarr; Reject (strictest outcome wins)</li>
 * </ul>
 */
class HealthUnderwritingRuleTest {

    private HealthUnderwritingEvaluator evaluator;
    private final HealthRules standardRules = new HealthRules(70, 60, true);

    @BeforeEach
    void setUp() {
        evaluator = new HealthUnderwritingEvaluator(new ObjectMapper());
    }

    @ParameterizedTest(name = "Age {0}, preExisting={1} -> Outcome {2}")
    @CsvSource({
            "45, false, APPROVE",
            "59, false, APPROVE",
            "60, false, REFER",
            "70, false, REFER",
            "71, false, REJECT",
            "30, true,  REFER",
            "72, true,  REJECT"
    })
    @DisplayName("TC-UW-02: Health age boundaries and single-applicant pre-existing rules")
    void healthAgeAndPreExistingBoundaries(int age, boolean preExisting, UnderwritingOutcome expectedOutcome) {
        Application application = new Application();
        application.setDetailsJson("{\"age\":" + age + ",\"preExisting\":" + preExisting + "}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(expectedOutcome);
        assertThat(result.getReason()).isNotEmpty();
    }

    @Test
    @DisplayName("Precedence: Age > 70 with pre-existing disease yields REJECT (Reject > Refer)")
    void ageOver70WithPreExistingYieldsReject() {
        Application application = new Application();
        application.setDetailsJson("{\"age\":75,\"preExisting\":true}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).containsIgnoringCase("exceeds");
    }

    @Test
    @DisplayName("Multi-member application uses the OLDEST member's age for rule evaluation")
    void usesOldestMemberAge() {
        Application application = new Application();
        // Members: 28, 35, 62. Oldest member is 62 -> Should trigger REFER (60-70 range)
        application.setDetailsJson("{\"members\":[{\"age\":28},{\"age\":35},{\"age\":62}],\"preExisting\":false}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).contains("62");
    }

    @Test
    @DisplayName("Multi-member application with oldest member over 70 yields REJECT")
    void multiMemberWithSeniorOver70YieldsReject() {
        Application application = new Application();
        // Members: 30, 42, 73. Oldest member is 73 -> Should trigger REJECT
        application.setDetailsJson("{\"members\":[{\"age\":30},{\"age\":42},{\"age\":73}],\"preExisting\":false}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REJECT);
        assertThat(result.getReason()).contains("73");
    }

    @Test
    @DisplayName("Multi-member application with all members under 60 and no pre-existing yields APPROVE")
    void multiMemberAllUnder60Approved() {
        Application application = new Application();
        application.setDetailsJson("{\"members\":[{\"age\":25},{\"age\":32},{\"age\":55}],\"preExisting\":false}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.APPROVE);
    }

    @Test
    @DisplayName("Pre-existing flag declared in any member triggers REFER for young applicants")
    void preExistingDeclaredInChildMemberTriggersRefer() {
        Application application = new Application();
        // Ages: 30, 35. One member has preExisting condition -> Should trigger REFER
        application.setDetailsJson("{\"members\":[{\"age\":30,\"preExisting\":false},{\"age\":35,\"preExisting\":true}]}");

        UnderwritingResult result = evaluator.evaluate(application, standardRules);

        assertThat(result.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result.getReason()).containsIgnoringCase("pre-existing");
    }

    @Test
    @DisplayName("Missing health rule configuration yields REFER with expected reason")
    void missingHealthRuleConfigYieldsRefer() {
        Application application = new Application();
        application.setDetailsJson("{\"age\":45,\"preExisting\":false}");

        UnderwritingResult result1 = evaluator.evaluate(application, null);
        assertThat(result1.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result1.getReason()).isEqualTo("Missing rule configuration");

        UnderwritingResult result2 = evaluator.evaluate(application, new HealthRules(null, 60, true));
        assertThat(result2.getOutcome()).isEqualTo(UnderwritingOutcome.REFER);
        assertThat(result2.getReason()).isEqualTo("Missing rule configuration");
    }
}
