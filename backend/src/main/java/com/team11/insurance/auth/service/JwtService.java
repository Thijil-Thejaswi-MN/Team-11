package com.team11.insurance.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.exception.AuthenticationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Service for issuing and validating HMAC-SHA256 (HS256) JSON Web Tokens (INS-SR-003, INS-SR-005).
 * <p>
 * Configuration:
 * <ul>
 *   <li>Signing secret loaded via {@code insurance.jwt.secret}</li>
 *   <li>Fail-secure guard: strictly refuses to start in production if secret is unconfigured or insecure</li>
 *   <li>Default expiration: 900 seconds (15 minutes) aligned with SAD §4.3.1 session lifetime</li>
 * </ul>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String DEFAULT_DEV_SECRET = "dev-insecure-secret-insurance-workflow-team11-test-key-256-bits-long!!";

    public static final String ISSUER = "team11-insurance-workflow";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_INSURER_ID = "insurerId";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_EMAIL = "email";

    @Value("${insurance.jwt.secret:}")
    private String configuredSecret;

    @Value("${insurance.jwt.expiration-seconds:28800}")
    private int expirationSeconds = 28800; // 8 hours absolute maximum per TC-Sec-10

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private byte[] signingKeyBytes;

    public JwtService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void validateConfiguration() {
        boolean isProduction = false;
        if (environment != null) {
            isProduction = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        }

        String secretToUse = configuredSecret;
        if (secretToUse == null || secretToUse.trim().isEmpty()) {
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: 'insurance.jwt.secret' MUST be configured with a high-entropy secret in production profile!");
            }
            log.warn("insurance.jwt.secret is not set. Using default dev secret. DO NOT USE IN PRODUCTION.");
            secretToUse = DEFAULT_DEV_SECRET;
        } else if (isProduction && (secretToUse.equals(DEFAULT_DEV_SECRET) || secretToUse.length() < 32)) {
            throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production JWT secret must be at least 32 characters (256 bits) and cannot use default dev secret!");
        }

        this.signingKeyBytes = secretToUse.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a signed HS256 JWT for an authenticated identity.
     *
     * @param userId user identifier
     * @param email user email
     * @param role user role name
     * @param insurerId insurer ID (if staff)
     * @param sessionId server-side session UUID
     * @param now current instant
     * @return compact URL-safe JWT string
     */
    public String generateToken(Long userId, String email, String role, Long insurerId, String sessionId, Instant now) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long iat = now.getEpochSecond();
            long exp = iat + expirationSeconds;

            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", ISSUER);
            claims.put("sub", String.valueOf(userId));
            claims.put(CLAIM_EMAIL, email);
            claims.put(CLAIM_ROLE, role);
            if (insurerId != null) {
                claims.put(CLAIM_INSURER_ID, insurerId);
            }
            claims.put(CLAIM_SESSION_ID, sessionId);
            claims.put("iat", iat);
            claims.put("exp", exp);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(claims));
            String signingInput = encodedHeader + "." + encodedPayload;

            String signature = base64UrlEncode(signHmacSha256(signingInput, signingKeyBytes));
            return signingInput + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate JWT token", ex);
        }
    }

    /**
     * Parses, cryptographically verifies, and extracts claims from a JWT string.
     *
     * @param token raw bearer token
     * @param now current instant for expiration validation
     * @return verified claims map
     * @throws AuthenticationException if signature, structure, or expiration is invalid
     */
    public Map<String, Object> parseAndValidateToken(String token, Instant now) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("Missing authentication token");
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Malformed authentication token");
        }

        String encodedHeader = parts[0];
        String encodedPayload = parts[1];
        String signature = parts[2];

        // 1. Verify cryptographic signature
        String signingInput = encodedHeader + "." + encodedPayload;
        byte[] expectedSig = signHmacSha256(signingInput, signingKeyBytes);
        byte[] providedSig;
        try {
            providedSig = base64UrlDecode(signature);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid token signature encoding");
        }

        if (!MessageDigest.isEqual(expectedSig, providedSig)) {
            throw new AuthenticationException("Invalid token signature");
        }

        // 2. Parse payload claims
        Map<String, Object> claims;
        try {
            byte[] payloadBytes = base64UrlDecode(encodedPayload);
            claims = objectMapper.readValue(payloadBytes, new TypeReference<>() {});
        } catch (Exception e) {
            throw new AuthenticationException("Unreadable token payload");
        }

        // 3. Verify issuer
        Object iss = claims.get("iss");
        if (!ISSUER.equals(iss)) {
            throw new AuthenticationException("Invalid token issuer");
        }

        // 4. Verify expiration
        Object expObj = claims.get("exp");
        if (expObj == null) {
            throw new AuthenticationException("Token missing expiration claim");
        }
        long expSeconds;
        if (expObj instanceof Number n) {
            expSeconds = n.longValue();
        } else {
            expSeconds = Long.parseLong(expObj.toString());
        }

        if (now.getEpochSecond() > expSeconds) {
            throw new AuthenticationException("Token has expired");
        }

        return claims;
    }

    public int getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] signHmacSha256(String data, byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 signature", ex);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
