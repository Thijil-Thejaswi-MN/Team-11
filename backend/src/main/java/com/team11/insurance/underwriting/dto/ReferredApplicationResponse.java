package com.team11.insurance.underwriting.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationStatus;

/**
 * Response DTO for referred applications awaiting underwriter review (INS-F-011, SAD §4.3.7).
 * Encapsulates the application fields required for underwriting risk assessment while avoiding
 * direct exposure of internal JPA entity state (e.g. database timestamps, Hibernate proxies)
 * and filtering out unnecessary sensitive personal data (e.g. PII, banking details, KYC tokens).
 */
public record ReferredApplicationResponse(
        Long id,
        Long customerId,
        Long insurerId,
        Long planId,
        Long quoteId,
        String productType,
        String vehicleRegNo,
        BigDecimal premium,
        String detailsJson,
        ApplicationStatus status,
        String reason,
        Instant createdAt
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ReferredApplicationResponse from(Application app) {
        if (app == null) {
            return null;
        }
        return new ReferredApplicationResponse(
                app.getId(),
                app.getCustomerId(),
                app.getInsurerId(),
                app.getPlanId(),
                app.getQuoteId(),
                app.getProductType(),
                app.getVehicleRegNo(),
                app.getPremium(),
                sanitizeDetailsJson(app.getDetailsJson()),
                app.getStatus(),
                app.getRejectionReason(),
                app.getCreatedAt()
        );
    }

    /**
     * Sanitizes raw application details JSON, keeping only fields strictly required
     * for underwriting assessment and enforcing documented types and shapes (fail-closed).
     * Strips all unknown fields, malformed shapes, and sensitive personal or financial information.
     */
    public static String sanitizeDetailsJson(String rawJson) {
        if (rawJson == null) {
            return null;
        }
        if (rawJson.isBlank()) {
            return "{}";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);
            if (!root.isObject()) {
                return "{}";
            }
            ObjectNode sanitized = OBJECT_MAPPER.createObjectNode();
            root.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode val = entry.getValue();
                if (val == null || val.isNull()) {
                    return;
                }

                switch (key) {
                    case "regYear", "vehicleAge", "age", "applicantAge" -> {
                        if (val.isIntegralNumber()) {
                            sanitized.set(key, val);
                        }
                    }
                    case "idv", "sumInsured" -> {
                        if (val.isNumber()) {
                            sanitized.set(key, val);
                        }
                    }
                    case "vehicleType", "make", "model", "city" -> {
                        if (val.isTextual()) {
                            sanitized.set(key, val);
                        }
                    }
                    case "preExisting", "hasPreExisting" -> {
                        if (val.isBoolean() || val.isTextual()) {
                            sanitized.set(key, val);
                        }
                    }
                    case "addOns" -> {
                        if (val.isArray()) {
                            ArrayNode sanitizedAddOns = OBJECT_MAPPER.createArrayNode();
                            for (JsonNode item : val) {
                                if (item != null && item.isTextual()) {
                                    sanitizedAddOns.add(item.asText());
                                }
                            }
                            sanitized.set("addOns", sanitizedAddOns);
                        }
                    }
                    case "members" -> {
                        if (val.isArray()) {
                            ArrayNode sanitizedMembers = OBJECT_MAPPER.createArrayNode();
                            for (JsonNode member : val) {
                                if (member != null && member.isObject()) {
                                    ObjectNode sanitizedMember = OBJECT_MAPPER.createObjectNode();
                                    member.fields().forEachRemaining(mEntry -> {
                                        String mKey = mEntry.getKey();
                                        JsonNode mVal = mEntry.getValue();
                                        if (mVal == null || mVal.isNull()) {
                                            return;
                                        }
                                        switch (mKey) {
                                            case "age", "memberAge" -> {
                                                if (mVal.isIntegralNumber()) {
                                                    sanitizedMember.set(mKey, mVal);
                                                }
                                            }
                                            case "relation" -> {
                                                if (mVal.isTextual()) {
                                                    sanitizedMember.set(mKey, mVal);
                                                }
                                            }
                                            case "preExisting" -> {
                                                if (mVal.isBoolean() || mVal.isTextual()) {
                                                    sanitizedMember.set(mKey, mVal);
                                                }
                                            }
                                            default -> {
                                                // Fail-closed: ignore unallowed or sensitive member fields
                                            }
                                        }
                                    });
                                    sanitizedMembers.add(sanitizedMember);
                                }
                            }
                            sanitized.set("members", sanitizedMembers);
                        }
                    }
                    default -> {
                        // Fail-closed: ignore unallowed or sensitive top-level fields
                    }
                }
            });
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "{}";
        }
    }
}
