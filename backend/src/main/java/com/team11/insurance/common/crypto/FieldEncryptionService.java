package com.team11.insurance.common.crypto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static volatile FieldEncryptionService defaultInstance;

    @Value("${insurance.security.encryption-key:${insurance.encryption.key:}}")
    private String configuredKey;

    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public FieldEncryptionService() {
        this(null);
    }

    public FieldEncryptionService(Environment environment) {
        this.environment = environment;
    }

    public FieldEncryptionService(Environment environment, String explicitKey) {
        this.environment = environment;
        this.configuredKey = explicitKey;
        initKey(explicitKey);
    }

    @PostConstruct
    public void validateConfiguration() {
        boolean isProduction = false;
        if (environment != null) {
            isProduction = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        }

        String keyToUse = configuredKey;
        if (keyToUse == null || keyToUse.trim().isEmpty()) {
            if (isProduction) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: 'insurance.security.encryption-key' MUST be configured in production profile!");
            }
            log.warn("insurance.security.encryption-key is not set. Using default dev encryption key. DO NOT USE IN PRODUCTION.");
            keyToUse = DEFAULT_DEV_KEY;
        } else if (isProduction && (keyToUse.equals(DEFAULT_DEV_KEY) || getRawKeyBytes(keyToUse).length != 32)) {
            throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production encryption key must be 256 bits (32 bytes) and cannot use default dev key!");
        }

        initKey(keyToUse);
        defaultInstance = this;
    }

    private void initKey(String keyString) {
        byte[] keyBytes = getRawKeyBytes(keyString);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 bytes (256 bits). Found: " + keyBytes.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    private byte[] getRawKeyBytes(String keyString) {
        if (keyString == null || keyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key cannot be null or empty.");
        }
        String trimmed = keyString.trim();
        // Check if key is Base64 encoded 32 bytes
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not base64, fall back to UTF-8
        }
        return trimmed.getBytes(StandardCharsets.UTF_8);
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
                    String activeProfiles = System.getProperty("spring.profiles.active", "");
                    if (activeProfiles.contains("prod") || activeProfiles.contains("production")) {
                        throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Cannot create default dev FieldEncryptionService instance in production!");
                    }
                    FieldEncryptionService devInstance = new FieldEncryptionService(null, DEFAULT_DEV_KEY);
                    defaultInstance = devInstance;
                }
            }
        }
        return defaultInstance;
    }

    public static void setDefaultInstance(FieldEncryptionService instance) {
        defaultInstance = instance;
    }
}
