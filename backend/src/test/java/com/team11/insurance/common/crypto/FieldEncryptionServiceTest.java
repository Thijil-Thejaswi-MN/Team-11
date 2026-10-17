package com.team11.insurance.common.crypto;

import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.kyc.KycStatus;
import com.team11.insurance.kyc.entity.KycRecord;
import com.team11.insurance.kyc.repository.KycRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("INS-SR-004 / TC-Sec-04: AES-256-GCM Field Encryption at Rest Tests")
class FieldEncryptionServiceTest {

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private EntityManager entityManager;

    private FieldEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        encryptionService = new FieldEncryptionService(env);
        encryptionService.validateConfiguration();
        EncryptedStringConverter.setStaticEncryptionService(encryptionService);
        kycRepository.deleteAll();
    }

    @Test
    @DisplayName("Encrypt and decrypt round-trip recovers original PAN and Aadhaar plaintext")
    void testEncryptDecryptRoundTrip() {
        String syntheticPan = "ABCDE1234F";
        String syntheticAadhaar = "987654321012";

        String encPan = encryptionService.encrypt(syntheticPan);
        String encAadhaar = encryptionService.encrypt(syntheticAadhaar);

        assertNotNull(encPan);
        assertNotNull(encAadhaar);
        assertTrue(encPan.startsWith(FieldEncryptionService.VERSION_PREFIX));
        assertTrue(encAadhaar.startsWith(FieldEncryptionService.VERSION_PREFIX));
        assertNotEquals(syntheticPan, encPan);
        assertNotEquals(syntheticAadhaar, encAadhaar);

        assertEquals(syntheticPan, encryptionService.decrypt(encPan));
        assertEquals(syntheticAadhaar, encryptionService.decrypt(encAadhaar));
    }

    @Test
    @DisplayName("Fresh unpredictable nonce: encrypting identical plaintext twice produces distinct ciphertexts")
    void testUniqueNonceProducesDistinctCiphertexts() {
        String plaintext = "ABCDE1234F";

        String ciphertext1 = encryptionService.encrypt(plaintext);
        String ciphertext2 = encryptionService.encrypt(plaintext);

        assertNotEquals(ciphertext1, ciphertext2, "AES-GCM MUST use a unique IV for each encryption operation");
        assertEquals(plaintext, encryptionService.decrypt(ciphertext1));
        assertEquals(plaintext, encryptionService.decrypt(ciphertext2));
    }

    @Test
    @DisplayName("Persisted PAN and Aadhaar are ciphertext in database, not equal to plaintext inputs")
    void testPersistenceStoresCiphertext() {
        String syntheticPan = "KLMNO5678P";
        String syntheticAadhaar = "123456789012";

        KycRecord record = new KycRecord(101L, syntheticPan, syntheticAadhaar, KycStatus.PENDING);
        kycRepository.saveAndFlush(record);
        entityManager.clear();

        // 1. Direct native database inspection: columns MUST contain versioned ciphertext
        Object[] rawRow = (Object[]) entityManager.createNativeQuery(
                "SELECT pan_number, aadhaar_number FROM kyc_records WHERE user_id = 101")
                .getSingleResult();

        String rawPanInDb = (String) rawRow[0];
        String rawAadhaarInDb = (String) rawRow[1];

        assertNotNull(rawPanInDb);
        assertNotNull(rawAadhaarInDb);
        assertTrue(rawPanInDb.startsWith("v1:"), "Raw PAN in DB must start with version prefix 'v1:'");
        assertTrue(rawAadhaarInDb.startsWith("v1:"), "Raw Aadhaar in DB must start with version prefix 'v1:'");
        assertNotEquals(syntheticPan, rawPanInDb, "Raw PAN in database MUST NOT equal plaintext input!");
        assertNotEquals(syntheticAadhaar, rawAadhaarInDb, "Raw Aadhaar in database MUST NOT equal plaintext input!");

        // 2. Entity retrieval transparently decrypts for authorized service access
        KycRecord loaded = kycRepository.findByUserId(101L).orElseThrow();
        assertEquals(syntheticPan, loaded.getPanNumber());
        assertEquals(syntheticAadhaar, loaded.getAadhaarNumber());
    }

    @Test
    @DisplayName("Tampered ciphertext fails authentication (AEADBadTagException) and is not returned as valid plaintext")
    void testTamperedCiphertextThrowsSecurityException() {
        String plaintext = "ABCDE1234F";
        String ciphertext = encryptionService.encrypt(plaintext);

        // Tamper with the base64 payload bytes
        String prefix = FieldEncryptionService.VERSION_PREFIX;
        String b64 = ciphertext.substring(prefix.length());
        byte[] bytes = Base64.getUrlDecoder().decode(b64);

        // Flip bits in the ciphertext / tag region
        bytes[bytes.length - 1] ^= 0xFF;
        String tamperedCiphertext = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        SecurityException ex = assertThrows(SecurityException.class, () -> encryptionService.decrypt(tamperedCiphertext));
        assertTrue(ex.getMessage().contains("tampered with or corrupted"));
    }

    @Test
    @DisplayName("Unencrypted or unsupported ciphertext format fails with migration required exception")
    void testUnencryptedLegacyDataRequiresMigration() {
        String legacyPlaintext = "ABCDE1234F";

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> encryptionService.decrypt(legacyPlaintext));
        assertTrue(ex.getMessage().contains("Data compatibility violation"));
    }

    @Test
    @DisplayName("Production profile fails startup safely if encryption key is missing, default, or wrong length")
    void testProductionFailSecureStartup() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // 1. Missing key in production throws IllegalStateException
        FieldEncryptionService missingKeyService = new FieldEncryptionService(prodEnv);
        IllegalStateException exMissing = assertThrows(IllegalStateException.class, missingKeyService::validateConfiguration);
        assertTrue(exMissing.getMessage().contains("CRITICAL SECURITY VIOLATION"));

        // 2. Default dev key in production throws IllegalStateException
        IllegalStateException exDev = assertThrows(IllegalStateException.class, () -> {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("prod");
            FieldEncryptionService svc = new FieldEncryptionService(env, FieldEncryptionService.DEFAULT_DEV_KEY);
            svc.validateConfiguration();
        });
        assertTrue(exDev.getMessage().contains("CRITICAL SECURITY VIOLATION"));

        // 3. Key shorter than 32 bytes throws IllegalArgumentException
        IllegalArgumentException exShort = assertThrows(IllegalArgumentException.class, () -> {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("prod");
            new FieldEncryptionService(env, "short-key-less-than-32-bytes");
        });
        assertTrue(exShort.getMessage().contains("Encryption key must be exactly 32 bytes"));
    }

    @Test
    @DisplayName("Accidental logging: KycRecord.toString() masks PAN and Aadhaar and never leaks plaintext")
    void testKycRecordToStringMasksSensitiveFields() {
        KycRecord record = new KycRecord(505L, "ABCDE1234F", "123456789012", KycStatus.PENDING);

        String toStringOutput = record.toString();
        assertFalse(toStringOutput.contains("ABCDE1234F"), "toString() must never contain plaintext PAN");
        assertFalse(toStringOutput.contains("123456789012"), "toString() must never contain plaintext Aadhaar");
        assertTrue(toStringOutput.contains("ABCDE****F"), "toString() must contain masked PAN");
        assertTrue(toStringOutput.contains("********9012"), "toString() must contain masked Aadhaar");
    }
}
