package com.team11.insurance.filestore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.FileValidationException;
import com.team11.insurance.common.exception.ResourceNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-06 DiskFileStore Unit & Integration Tests")
class DiskFileStoreTest {

    @TempDir
    Path tempStorageDir;

    private DiskFileStore fileStore;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // Valid sample payloads
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4 sample pdf content for testing".getBytes(StandardCharsets.UTF_8);
    private static final byte[] VALID_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};

    private CurrentUser customerUser;
    private CurrentUser otherCustomerUser;
    private CurrentUser claimsOfficerUser;
    private CurrentUser systemAdminUser;

    @BeforeEach
    void setUp() {
        fileStore = new DiskFileStore(tempStorageDir.toString(), "0123456789abcdef0123456789abcdef", objectMapper);
        fileStore.init();

        customerUser = new CurrentUser(1001L, UserRole.CUSTOMER, null);
        otherCustomerUser = new CurrentUser(1002L, UserRole.CUSTOMER, null);
        claimsOfficerUser = new CurrentUser(2001L, UserRole.CLAIMS_OFFICER, 1L);
        systemAdminUser = new CurrentUser(9999L, UserRole.SYSTEM_ADMIN, null);
    }

    @AfterEach
    void tearDown() {
        fileStore.clearAll();
    }

    @Test
    @DisplayName("storeStream() and get() round trip stores and retrieves decrypted document")
    void testStoreAndRetrieveRoundTrip() throws Exception {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "policy_document.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.POLICY,
                customerUser.getUserId()
        );

        assertNotNull(stored);
        assertNotNull(stored.id());
        assertEquals("policy_document.pdf", stored.originalFilename());
        assertEquals("application/pdf", stored.contentType());
        assertEquals(DocumentCategory.POLICY, stored.category());
        assertEquals(customerUser.getUserId(), stored.ownerUserId());

        // Verify retrieval by owner
        LoadedDocument loaded = fileStore.get(stored.id(), customerUser);
        assertNotNull(loaded);
        assertEquals(stored.id(), loaded.metadata().id());

        Resource res = loaded.resource();
        byte[] retrievedBytes;
        try (InputStream is = res.getInputStream()) {
            retrievedBytes = is.readAllBytes();
        }
        assertArrayEquals(VALID_PDF_BYTES, retrievedBytes, "Decrypted bytes must match original input");
    }

    @Test
    @DisplayName("Stored document is encrypted at rest on disk (raw disk file != plaintext)")
    void testEncryptionAtRest() throws Exception {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "encrypted_test.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.POLICY,
                customerUser.getUserId()
        );

        Path physicalPath = tempStorageDir.resolve("policies").resolve(stored.storageKey());
        assertTrue(Files.exists(physicalPath), "Encrypted file must exist in category subfolder");

        byte[] rawDiskBytes = Files.readAllBytes(physicalPath);
        assertFalse(new String(rawDiskBytes, StandardCharsets.UTF_8).contains("%PDF-1.4"),
                "Disk file content must be encrypted, not raw plaintext");
        assertNotEquals(VALID_PDF_BYTES.length, rawDiskBytes.length,
                "Encrypted file includes GCM IV (12 bytes) and auth tag (16 bytes)");
    }

    @Test
    @DisplayName("put() multipart upload returns unique database ID")
    void testPutMultipartUpload() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "kyc_id.png",
                "image/png",
                VALID_PNG_BYTES
        );

        Long docId = fileStore.put(multipartFile, DocumentCategory.KYC, customerUser);
        assertNotNull(docId);
        assertTrue(fileStore.exists(docId));

        LoadedDocument loaded = fileStore.get(docId, customerUser);
        assertEquals("kyc_id.png", loaded.metadata().originalFilename());
        assertEquals(DocumentCategory.KYC, loaded.metadata().category());
    }

    @Test
    @DisplayName("delete() removes document from storage and metadata index")
    void testDeleteAndMissingFile() {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_JPEG_BYTES),
                "claim_photo.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES.length,
                DocumentCategory.CLAIM,
                customerUser.getUserId()
        );

        assertTrue(fileStore.exists(stored.id()));

        fileStore.delete(stored.id(), customerUser);

        assertFalse(fileStore.exists(stored.id()));
        assertThrows(ResourceNotFoundException.class, () -> fileStore.get(stored.id(), customerUser));
        assertThrows(ResourceNotFoundException.class, () -> fileStore.delete(stored.id(), customerUser));
    }

    @Test
    @DisplayName("Oversized uploads (> 5 MB) are refused with HTTP 413 / FILE-413 (STP TC-Sec-07)")
    void testOversizedUploadRefused() {
        long oversized = 6 * 1024 * 1024L; // 6 MB fixture per TC-Sec-07
        byte[] bigDummy = new byte[100]; // Stream length claimed as 6MB

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                fileStore.storeStream(
                        new ByteArrayInputStream(bigDummy),
                        "huge.pdf",
                        "application/pdf",
                        oversized,
                        DocumentCategory.POLICY,
                        customerUser.getUserId()
                )
        );

        assertEquals(ErrorCode.FILE_413, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("Disallowed file types are refused with HTTP 415 / FILE-415 (STP TC-Sec-07)")
    void testDisallowedFileTypes() {
        byte[] exeBytes = "MZ executable binary".getBytes(StandardCharsets.UTF_8);

        // Disallowed extension
        FileValidationException ex1 = assertThrows(FileValidationException.class, () ->
                fileStore.storeStream(
                        new ByteArrayInputStream(exeBytes),
                        "malware.exe",
                        "application/octet-stream",
                        exeBytes.length,
                        DocumentCategory.KYC,
                        customerUser.getUserId()
                )
        );
        assertEquals(ErrorCode.FILE_415, ex1.getErrorCode());

        // Renamed executable: .pdf extension with non-PDF content (magic bytes check)
        FileValidationException ex2 = assertThrows(FileValidationException.class, () ->
                fileStore.storeStream(
                        new ByteArrayInputStream(exeBytes),
                        "disguised.pdf",
                        "application/pdf",
                        exeBytes.length,
                        DocumentCategory.KYC,
                        customerUser.getUserId()
                )
        );
        assertEquals(ErrorCode.FILE_415, ex2.getErrorCode());
        assertTrue(ex2.getMessage().contains("Disguised file"));
    }

    @Test
    @DisplayName("Path traversal attempts are blocked and rejected")
    void testPathTraversalDefense() {
        assertThrows(FileValidationException.class, () ->
                fileStore.storeStream(
                        new ByteArrayInputStream(VALID_PDF_BYTES),
                        "../../etc/passwd.pdf",
                        "application/pdf",
                        VALID_PDF_BYTES.length,
                        DocumentCategory.KYC,
                        customerUser.getUserId()
                )
        );

        assertThrows(FileValidationException.class, () ->
                fileStore.storeStream(
                        new ByteArrayInputStream(VALID_PDF_BYTES),
                        "..\\windows\\system32.pdf",
                        "application/pdf",
                        VALID_PDF_BYTES.length,
                        DocumentCategory.KYC,
                        customerUser.getUserId()
                )
        );
    }

    @Test
    @DisplayName("Authorization enforcement: customer cannot read another customer's files; staff & admin can read")
    void testAuthorizationEnforcement() {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "customer_doc.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.KYC,
                customerUser.getUserId()
        );

        // Owner can access
        assertDoesNotThrow(() -> fileStore.get(stored.id(), customerUser));

        // Different customer is denied with HTTP 403 / ACCESS-403
        assertThrows(AccessDeniedException.class, () -> fileStore.get(stored.id(), otherCustomerUser));
        assertThrows(AccessDeniedException.class, () -> fileStore.delete(stored.id(), otherCustomerUser));

        // Claims officer (staff) can access
        assertDoesNotThrow(() -> fileStore.get(stored.id(), claimsOfficerUser));

        // System Admin can access
        assertDoesNotThrow(() -> fileStore.get(stored.id(), systemAdminUser));
    }

    @Test
    @DisplayName("Injected Clock is used for upload timestamp")
    void testInjectedClockForUploadTimestamp() {
        Instant testInstant = Instant.parse("2026-10-21T15:30:00Z");
        Clock fixedClock = Clock.fixed(testInstant, ZoneOffset.UTC);
        DiskFileStore customClockStore = new DiskFileStore(tempStorageDir.toString(), "0123456789abcdef0123456789abcdef", null, fixedClock, objectMapper);
        customClockStore.init();

        StoredDocument stored = customClockStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "clock_test.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.POLICY,
                customerUser.getUserId()
        );

        assertEquals(testInstant, stored.uploadedAt(), "Upload timestamp must come directly from injected Clock");
    }

    @Test
    @DisplayName("Production profile fails startup if encryption key is missing or invalid length")
    void testProductionRejectsMissingOrInvalidKey() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // Missing/null key in production
        IllegalStateException exNull = assertThrows(IllegalStateException.class, () ->
                new DiskFileStore(tempStorageDir.toString(), null, prodEnv, Clock.systemDefaultZone(), objectMapper));
        assertTrue(exNull.getMessage().contains("Production requires a 256-bit AES encryption key"));

        // Blank key in production
        IllegalStateException exBlank = assertThrows(IllegalStateException.class, () ->
                new DiskFileStore(tempStorageDir.toString(), "   ", prodEnv, Clock.systemDefaultZone(), objectMapper));
        assertTrue(exBlank.getMessage().contains("Production requires a 256-bit AES encryption key"));

        // Malformed / wrong length key in production
        IllegalStateException exShort = assertThrows(IllegalStateException.class, () ->
                new DiskFileStore(tempStorageDir.toString(), "too-short-key", prodEnv, Clock.systemDefaultZone(), objectMapper));
        assertTrue(exShort.getMessage().contains("must be exactly 32 bytes"));

        // Valid 32-byte key in production succeeds
        assertDoesNotThrow(() ->
                new DiskFileStore(tempStorageDir.toString(), "0123456789abcdef0123456789abcdef", prodEnv, Clock.systemDefaultZone(), objectMapper));

        // Valid 64-character hex key in production succeeds
        assertDoesNotThrow(() ->
                new DiskFileStore(tempStorageDir.toString(), "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcde0", prodEnv, Clock.systemDefaultZone(), objectMapper));

        // Compose default hex key in production fails closed
        IllegalStateException exComposeDev = assertThrows(IllegalStateException.class, () ->
                new DiskFileStore(tempStorageDir.toString(), DiskFileStore.DEFAULT_COMPOSE_HEX_KEY, prodEnv, Clock.systemDefaultZone(), objectMapper));
        assertTrue(exComposeDev.getMessage().contains("Default dev key cannot be used in production"));

        // Invalid 64-character non-hex key in production fails closed
        IllegalStateException exNonHex = assertThrows(IllegalStateException.class, () ->
                new DiskFileStore(tempStorageDir.toString(), "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", prodEnv, Clock.systemDefaultZone(), objectMapper));
        assertTrue(exNonHex.getMessage().contains("must be exactly 32 bytes"));
    }

    @Test
    @DisplayName("Dev profile with Compose default 64-character hex key initializes and performs file round trip")
    void testDevProfileWithComposeDefaultHexKey() throws Exception {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        DiskFileStore devStore = new DiskFileStore(tempStorageDir.toString(), DiskFileStore.DEFAULT_COMPOSE_HEX_KEY, devEnv, Clock.systemDefaultZone(), objectMapper);
        devStore.init();

        StoredDocument stored = devStore.storeStream(
                new java.io.ByteArrayInputStream(VALID_PDF_BYTES),
                "compose_key_test.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.POLICY,
                customerUser.getUserId()
        );

        assertNotNull(stored);
        LoadedDocument loaded = devStore.get(stored.id(), customerUser);
        assertNotNull(loaded);
        try (java.io.InputStream is = loaded.resource().getInputStream()) {
            assertArrayEquals(VALID_PDF_BYTES, is.readAllBytes());
        }
    }

    @Test
    @DisplayName("Dev and test profiles permit fallback to default development key when key is absent")
    void testDevAndTestProfilePermitDefaultKeyFallback() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        assertDoesNotThrow(() ->
                new DiskFileStore(tempStorageDir.toString(), null, devEnv, Clock.systemDefaultZone(), objectMapper));

        MockEnvironment testEnv = new MockEnvironment();
        testEnv.setActiveProfiles("test");

        assertDoesNotThrow(() ->
                new DiskFileStore(tempStorageDir.toString(), "", testEnv, Clock.systemDefaultZone(), objectMapper));
    }

    @Test
    @DisplayName("Missing/null user context fails closed for normal get() and delete() operations")
    void testMissingUserContextFailsClosed() {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "auth_fail_doc.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.KYC,
                customerUser.getUserId()
        );

        // get() with null user context throws AccessDeniedException
        AccessDeniedException getEx = assertThrows(AccessDeniedException.class, () -> fileStore.get(stored.id(), null));
        assertTrue(getEx.getMessage().contains("user context is required"));

        // delete() with null user context throws AccessDeniedException
        AccessDeniedException delEx = assertThrows(AccessDeniedException.class, () -> fileStore.delete(stored.id(), null));
        assertTrue(delEx.getMessage().contains("user context is required"));
    }

    @Test
    @DisplayName("Explicit internal trusted path (getInternal, deleteInternal) succeeds without user context")
    void testInternalTrustedPath() throws Exception {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "internal_doc.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.KYC,
                customerUser.getUserId()
        );

        // getInternal works without user context
        LoadedDocument loaded = fileStore.getInternal(stored.id());
        assertNotNull(loaded);
        assertEquals(stored.id(), loaded.metadata().id());

        // deleteInternal works without user context
        fileStore.deleteInternal(stored.id());
        assertFalse(fileStore.exists(stored.id()));
    }

    @Test
    @DisplayName("Tampered encrypted file on disk fails decryption with AEAD / SecurityException (INS-SR-004)")
    void testTamperedEncryptedFileFailsDecryption() throws Exception {
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "tamper_proof.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.POLICY,
                customerUser.getUserId()
        );

        Path encFile = tempStorageDir.resolve("policies").resolve(stored.storageKey());
        assertTrue(Files.exists(encFile), "Encrypted file must exist");

        byte[] encBytes = Files.readAllBytes(encFile);
        // Flip bits in the ciphertext / GCM auth tag portion
        encBytes[encBytes.length - 1] ^= 0x5A;
        Files.write(encFile, encBytes);

        com.team11.insurance.common.exception.FileStorageException ex =
                assertThrows(com.team11.insurance.common.exception.FileStorageException.class,
                        () -> fileStore.get(stored.id(), customerUser));

        assertTrue(ex.getMessage().contains("Failed to decrypt or read document"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof SecurityException, "Cause must be SecurityException due to tag validation failure");
        assertTrue(ex.getCause().getMessage().contains("tampered with or corrupted"));
    }

    @Test
    @DisplayName("Encryption keys are configured externally and never stored alongside encrypted documents on disk")
    void testEncryptionKeyNotStoredAlongsideEncryptedFiles() throws Exception {
        String testKey = "0123456789abcdef0123456789abcdef";
        StoredDocument stored = fileStore.storeStream(
                new ByteArrayInputStream(VALID_PDF_BYTES),
                "key_isolation.pdf",
                "application/pdf",
                VALID_PDF_BYTES.length,
                DocumentCategory.CLAIM,
                customerUser.getUserId()
        );

        Path encFile = tempStorageDir.resolve("claims").resolve(stored.storageKey());
        Path metaFile = tempStorageDir.resolve("claims").resolve(stored.storageKey().replace(".enc", ".meta"));

        assertTrue(Files.exists(encFile));
        assertTrue(Files.exists(metaFile));

        byte[] encBytes = Files.readAllBytes(encFile);
        byte[] metaBytes = Files.readAllBytes(metaFile);

        // Neither raw bytes of key nor key string can appear in file content
        assertFalse(new String(encBytes, StandardCharsets.ISO_8859_1).contains(testKey));
        assertFalse(new String(metaBytes, StandardCharsets.UTF_8).contains(testKey));
        assertFalse(new String(metaBytes, StandardCharsets.UTF_8).toLowerCase().contains("secretkey"));
    }
}
