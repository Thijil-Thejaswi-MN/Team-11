package com.team11.insurance.common.crypto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Production-grade AES-256-GCM authenticated field encryption service (INS-SR-004, SAD §4.4).
 * <p>
 * Key Security Controls:
 * <ul>
 *   <li>AES-256 authenticated encryption using GCM mode (128-bit authentication tag).</li>
 *   <li>Fresh, cryptographically strong 96-bit (12-byte) unpredictable nonce for every encryption operation.</li>
 *   <li>Versioned ciphertext format ({@code v1:...}) enabling key rotation and algorithm agility.</li>
 *   <li>Tamper-evident authentication tag verification during decryption; corrupted/tampered ciphertexts fail closed.</li>
 *   <li>Fail-secure startup verification in production profile: refuses startup if encryption key is missing or insecure.</li>
 *   <li>No sensitive plaintext, encryption keys, or nonces are ever written to application logs or error messages.</li>
 * </ul>
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);

    public static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final String KEY_ALGORITHM = "AES";
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final String VERSION_PREFIX = "v1:";

    public static final String DEFAULT_DEV_KEY = "dev-aes256-key-team11-workflow!!"; // exactly 32 bytes UTF-8
    public static final String DEFAULT_COMPOSE_HEX_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; // 64-char hex dev key
    public static final String DEFAULT_DEV_KEY_LEGACY = "0123456789abcdef0123456789abcdef"; // 32-byte UTF-8 dev key

    private static volatile FieldEncryptionService defaultInstance;

    @Value("${insurance.security.encryption-key:${insurance.encryption.key:}}")
    private String configuredKey;

    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    /**
     * Primary constructor used by Spring Framework for dependency injection and explicit key configuration.
     * Guaranteed to receive the active Spring {@link Environment} and configured encryption key.
     *
     * @param environment   active Spring environment (must not be null)
     * @param configuredKey encryption key configured via properties, environment variables, or explicit parameter
     */
    @Autowired
    public FieldEncryptionService(
            Environment environment,
            @Value("${insurance.security.encryption-key:${insurance.encryption.key:}}") String configuredKey) {
        if (environment == null) {
            throw new IllegalArgumentException("Environment must not be null");
        }
        this.environment = environment;
        this.configuredKey = (configuredKey != null && !configuredKey.trim().isEmpty())
                ? configuredKey.trim()
                : resolveKeyFromEnvironment(environment);
        if (this.configuredKey != null && !this.configuredKey.trim().isEmpty()) {
            initKey(this.configuredKey);
        }
    }

    /**
     * Constructor for environment-based initialization (e.g. unit and integration tests).
     *
     * @param environment active environment containing configuration properties
     */
    public FieldEncryptionService(Environment environment) {
        this(environment, (String) null);
    }

    @PostConstruct
    public void validateConfiguration() {
        if (this.environment == null) {
            throw new IllegalStateException("CRITICAL SECURITY VIOLATION: FieldEncryptionService environment must not be null!");
        }

        boolean isProduction = isProductionProfile(this.environment);

        String keyToUse = this.configuredKey;
        if (keyToUse == null || keyToUse.trim().isEmpty()) {
            keyToUse = resolveKeyFromEnvironment(this.environment);
        }

        if (keyToUse == null || keyToUse.trim().isEmpty()) {
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: 'insurance.security.encryption-key' MUST be configured in production profile! Fallback to default dev key is forbidden in production.");
            }
            log.warn("insurance.security.encryption-key is not set. Using default dev encryption key. DO NOT USE IN PRODUCTION.");
            keyToUse = DEFAULT_DEV_KEY;
        } else if (isProduction) {
            if (isDefaultDevKey(keyToUse)) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production encryption key cannot use default dev key! Configure a secure AES-256 key via 'insurance.security.encryption-key' or ENCRYPTION_KEY.");
            }
            byte[] rawBytes;
            try {
                rawBytes = getRawKeyBytes(keyToUse);
            } catch (Exception ex) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production encryption key is invalid or malformed!", ex);
            }
            if (rawBytes.length != 32) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production encryption key must be 256 bits (32 bytes)! Found: " + rawBytes.length + " bytes.");
            }
        }

        initKey(keyToUse);
        defaultInstance = this;
        EncryptedStringConverter.setStaticEncryptionService(this);
    }

    private static String resolveKeyFromEnvironment(Environment env) {
        if (env == null) {
            return null;
        }
        String key = env.getProperty("insurance.security.encryption-key");
        if (key == null || key.trim().isEmpty()) {
            key = env.getProperty("insurance.encryption.key");
        }
        if (key == null || key.trim().isEmpty()) {
            key = env.getProperty("ENCRYPTION_KEY");
        }
        return (key != null && !key.trim().isEmpty()) ? key.trim() : null;
    }

    public static boolean isProductionProfile(Environment environment) {
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            if (activeProfiles != null && activeProfiles.length > 0) {
                for (String p : activeProfiles) {
                    if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                        return true;
                    }
                }
            }
            String[] defaultProfiles = environment.getDefaultProfiles();
            if (defaultProfiles != null && defaultProfiles.length > 0) {
                for (String p : defaultProfiles) {
                    if ("prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p)) {
                        return true;
                    }
                }
            }
        }
        String sysProp = System.getProperty("spring.profiles.active");
        if (sysProp != null && containsProdProfile(sysProp)) {
            return true;
        }
        String envVar = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envVar != null && containsProdProfile(envVar)) {
            return true;
        }
        return false;
    }

    private static boolean containsProdProfile(String profileString) {
        if (profileString == null || profileString.trim().isEmpty()) {
            return false;
        }
        for (String part : profileString.split(",")) {
            String trimmed = part.trim();
            if ("prod".equalsIgnoreCase(trimmed) || "production".equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public boolean isProduction() {
        return isProductionProfile(this.environment);
    }

    private void initKey(String keyString) {
        byte[] keyBytes = getRawKeyBytes(keyString);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 bytes (256 bits). Found: " + keyBytes.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    public static boolean isDefaultDevKey(String key) {
        if (key == null) {
            return false;
        }
        String trimmed = key.trim();
        return DEFAULT_DEV_KEY.equals(trimmed)
                || DEFAULT_COMPOSE_HEX_KEY.equalsIgnoreCase(trimmed)
                || DEFAULT_DEV_KEY_LEGACY.equals(trimmed);
    }

    private byte[] getRawKeyBytes(String keyString) {
        if (keyString == null || keyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key cannot be null or empty.");
        }
        String trimmed = keyString.trim();

        // 1. Check if key is 64-character hexadecimal (256-bit AES key)
        if (trimmed.length() == 64 && isHex(trimmed)) {
            try {
                return HexFormat.of().parseHex(trimmed);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // 2. Check if key is Base64 encoded 32 bytes
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not base64, fall back to UTF-8
        }

        // 3. Fall back to raw UTF-8 bytes
        return trimmed.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a fresh unpredictable 12-byte IV.
     * Output format: {@code v1:<base64url(iv + ciphertext + tag)>}.
     *
     * @param plaintext sensitive plaintext to protect at rest
     * @return versioned ciphertext string or null if input is null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (secretKey == null) {
            throw new IllegalStateException("FieldEncryptionService secretKey is not initialized.");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] cipherAndTag = cipher.doFinal(plaintextBytes);

            byte[] combined = new byte[iv.length + cipherAndTag.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherAndTag, 0, combined, iv.length, cipherAndTag.length);

            return VERSION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt sensitive field at rest", ex);
        }
    }

    /**
     * Decrypts versioned ciphertext and verifies the GCM authentication tag.
     *
     * @param ciphertext versioned ciphertext (e.g. {@code v1:...})
     * @return recovered plaintext or null if input is null
     * @throws SecurityException if authentication tag check fails (tampered data)
     * @throws IllegalStateException on unsupported format or unencrypted legacy data requiring migration
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (secretKey == null) {
            throw new IllegalStateException("FieldEncryptionService secretKey is not initialized.");
        }

        if (!ciphertext.startsWith(VERSION_PREFIX)) {
            // Unencrypted legacy data or unsupported version: do not silently accept
            throw new IllegalStateException("Data compatibility violation: sensitive field is unencrypted or uses an unsupported ciphertext version. Data migration required.");
        }

        try {
            String payload = ciphertext.substring(VERSION_PREFIX.length());
            byte[] combined = Base64.getUrlDecoder().decode(payload);

            if (combined.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new SecurityException("Malformed ciphertext payload: insufficient length.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);

            int cipherAndTagLength = combined.length - GCM_IV_LENGTH_BYTES;
            byte[] cipherAndTag = new byte[cipherAndTagLength];
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherAndTag, 0, cipherAndTagLength);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(cipherAndTag);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException badTagEx) {
            // Tampered ciphertext or wrong key
            throw new SecurityException("Ciphertext authentication failed: data has been tampered with or corrupted.", badTagEx);
        } catch (SecurityException secEx) {
            throw secEx;
        } catch (Exception ex) {
            throw new SecurityException("Decryption failure for sensitive field", ex);
        }
    }

    /**
     * Provides a safe default instance for test/dev contexts and JPA converters.
     * Refuses to supply default instance in production profile.
     */
    public static FieldEncryptionService getDefaultInstance() {
        if (defaultInstance == null) {
            synchronized (FieldEncryptionService.class) {
                if (defaultInstance == null) {
                    if (isProductionProfile(null)) {
                        throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Cannot create default dev FieldEncryptionService instance in production profile!");
                    }
                    org.springframework.core.env.StandardEnvironment devEnv = new org.springframework.core.env.StandardEnvironment();
                    FieldEncryptionService devInstance = new FieldEncryptionService(devEnv, DEFAULT_DEV_KEY);
                    devInstance.validateConfiguration();
                    defaultInstance = devInstance;
                }
            }
        }
        return defaultInstance;
    }

    public static void setDefaultInstance(FieldEncryptionService instance) {
        synchronized (FieldEncryptionService.class) {
            defaultInstance = instance;
            EncryptedStringConverter.setStaticEncryptionService(instance);
        }
    }

    public static void resetDefaultInstance() {
        synchronized (FieldEncryptionService.class) {
            defaultInstance = null;
            EncryptedStringConverter.setStaticEncryptionService(null);
        }
    }

    @Override
    public String toString() {
        return "FieldEncryptionService[algorithm=" + KEY_ALGORITHM + ", transformation=" + CIPHER_TRANSFORMATION + "]";
    }
}
