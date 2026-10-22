package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads the premium rule JSON of a motor plan (RatingRules.getPremiumRulesJson()), for example
 * {"type":"R-M","ownDamageBasePct":0.02,"ageLoadingMinYears":6,"ageLoadingMaxYears":10,
 *  "ageLoadingPct":0.05,"addOns":{"ZERO_DEP":1080.00},"depreciationPct":0.10}
 */
public final class MotorRatingParametersParser {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private MotorRatingParametersParser() { }

    /** @throws IllegalArgumentException if the JSON is invalid or is not a motor (R-M) rule */
    public static MotorRatingParameters fromJson(String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Premium rules are not valid JSON", e);
        }
        if (root == null || !root.isObject() || !"R-M".equals(root.path("type").asText())) {
            throw new IllegalArgumentException("Premium rules are not a motor (R-M) rule");
        }
        Map<String, BigDecimal> addOns = new LinkedHashMap<>();
        JsonNode addOnNode = root.path("addOns");
        if (addOnNode.isObject()) {
            addOnNode.fields().forEachRemaining(e -> addOns.put(e.getKey(), number(e.getValue(), "addOns." + e.getKey())));
        }
        return new MotorRatingParameters(
                number(root.get("ownDamageBasePct"), "ownDamageBasePct"),
                number(root.get("ageLoadingMinYears"), "ageLoadingMinYears").intValueExact(),
                number(root.get("ageLoadingMaxYears"), "ageLoadingMaxYears").intValueExact(),
                number(root.get("ageLoadingPct"), "ageLoadingPct"),
                addOns,
                number(root.get("depreciationPct"), "depreciationPct"));
    }

    private static BigDecimal number(JsonNode node, String field) {
        if (node == null || !node.isNumber()) {
            throw new IllegalArgumentException("Premium rule field '" + field + "' is missing or not a number");
        }
        return node.decimalValue();
    }
}
