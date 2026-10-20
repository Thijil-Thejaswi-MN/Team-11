package com.team11.insurance.underwriting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

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

    /**
     * Whitelist of legitimate underwriting risk factors allowed in detailsJson.
     * Prevents leakage of unnecessary sensitive data (PII, banking, KYC documents).
     */
    private static final Set<String> ALLOWED_TOP_LEVEL_FIELDS = Set.of(
            "regYear", "vehicleAge", "vehicleType", "make", "model", "city", "idv", "addOns",
            "age", "applicantAge", "members", "preExisting", "hasPreExisting", "sumInsured"
    );

    private static final Set<String> ALLOWED_MEMBER_FIELDS = Set.of(
            "age", "memberAge", "relation", "preExisting"
    );

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
     * for underwriting assessment and stripping sensitive personal or financial information.
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
                if (ALLOWED_TOP_LEVEL_FIELDS.contains(key)) {
                    if ("members".equals(key) && entry.getValue().isArray()) {
                        ArrayNode sanitizedMembers = OBJECT_MAPPER.createArrayNode();
                        for (JsonNode member : entry.getValue()) {
                            if (member.isObject()) {
                                ObjectNode sanitizedMember = OBJECT_MAPPER.createObjectNode();
                                member.fields().forEachRemaining(mEntry -> {
                                    if (ALLOWED_MEMBER_FIELDS.contains(mEntry.getKey())) {
                                        sanitizedMember.set(mEntry.getKey(), mEntry.getValue());
                                    }
                                });
                                sanitizedMembers.add(sanitizedMember);
                            }
                        }
                        sanitized.set("members", sanitizedMembers);
                    } else {
                        sanitized.set(key, entry.getValue());
                    }
                }
            });
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "{}";
        }
    }
}
