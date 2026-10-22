package com.team11.insurance.quotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.ValidationException;

/**
 * Unit tests for {@link HealthRatingRules} (INS-F-006, P1-18, TC-Quote-02).
 * Verifies age band boundaries, sum insured multiplier lookups, and pre-existing disease multiplier.
 */
@DisplayName("Health Rating Rules Unit Tests (TC-Quote-02)")
class HealthRatingRulesTest {

    private HealthRatingRules ratingRules;

    @BeforeEach
    void setUp() {
        ratingRules = new HealthRatingRules();
    }

    @ParameterizedTest(name = "Age {0} -> expected base premium Rs {1}")
    @CsvSource({
            "18, 3000.00",
            "25, 3000.00",
            "35, 3000.00",
            "36, 4500.00",
            "40, 4500.00",
            "45, 4500.00",
            "46, 7000.00",
            "50, 7000.00",
            "59, 7000.00",
            "60, 10000.00",
            "70, 10000.00",
            "120, 10000.00"
    })
    @DisplayName("Age bracket boundaries: 18-35 (3k), 36-45 (4.5k), 46-59 (7k), 60+ (10k)")
    void ageBandBoundaries(int age, String expectedBase) {
        BigDecimal base = ratingRules.calculateMemberBase(age);
        assertThat(base).isEqualByComparingTo(new BigDecimal(expectedBase));
    }

    @Test
    @DisplayName("Age below minimum (17) throws ValidationException")
    void ageBelow18ThrowsException() {
        assertThatThrownBy(() -> ratingRules.calculateMemberBase(17))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No rating rule age band found for member age 17");
    }

    @Test
    @DisplayName("Age above maximum (121) throws ValidationException")
    void ageAbove120ThrowsException() {
        assertThatThrownBy(() -> ratingRules.calculateMemberBase(121))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No rating rule age band found for member age 121");
    }

    @Test
    @DisplayName("Supported sum insured: 5,00,000 has factor 1.0")
    void sumInsured5Lakh() {
        BigDecimal factor = ratingRules.getSumInsuredMultiplier(new BigDecimal("500000"));
        assertThat(factor).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("Supported sum insured: 10,00,000 has factor 1.6")
    void sumInsured10Lakh() {
        BigDecimal factor = ratingRules.getSumInsuredMultiplier(new BigDecimal("1000000"));
        assertThat(factor).isEqualByComparingTo(new BigDecimal("1.6"));
    }

    @Test
    @DisplayName("Unsupported sum insured: 7,50,000 throws ValidationException")
    void unsupportedSumInsuredThrowsException() {
        assertThatThrownBy(() -> ratingRules.getSumInsuredMultiplier(new BigDecimal("750000")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not offered by this plan");
    }

    @Test
    @DisplayName("Null or non-positive sum insured throws ValidationException")
    void invalidSumInsuredThrowsException() {
        assertThatThrownBy(() -> ratingRules.getSumInsuredMultiplier(null))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> ratingRules.getSumInsuredMultiplier(BigDecimal.ZERO))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> ratingRules.getSumInsuredMultiplier(new BigDecimal("-100000")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Pre-existing disease multiplier is 1.2")
    void preExistingMultiplier() {
        assertThat(ratingRules.getPreExistingMultiplier()).isEqualByComparingTo(new BigDecimal("1.2"));
    }

    @Test
    @DisplayName("Deserialization from JSON preserves rating rules configuration")
    void deserializationFromJson() throws Exception {
        String json = "{\"type\":\"R-H\",\"ageBands\":[{\"minAge\":18,\"maxAge\":35,\"base\":3000.00},{\"minAge\":36,\"maxAge\":45,\"base\":4500.00}],\"sumInsuredMultipliers\":{\"500000\":1.0,\"1000000\":1.6},\"preExistingMultiplier\":1.2}";
        ObjectMapper mapper = new ObjectMapper();
        HealthRatingRules rules = mapper.readValue(json, HealthRatingRules.class);

        assertThat(rules.getType()).isEqualTo("R-H");
        assertThat(rules.calculateMemberBase(30)).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(rules.calculateMemberBase(40)).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(rules.getSumInsuredMultiplier(new BigDecimal("500000"))).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(rules.getPreExistingMultiplier()).isEqualByComparingTo(new BigDecimal("1.2"));
    }
}
