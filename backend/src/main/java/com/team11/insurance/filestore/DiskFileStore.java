package com.team11.insurance.filestore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.FileStorageException;
import com.team11.insurance.common.exception.FileValidationException;
import com.team11.insurance.common.exception.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;

/**
 * Local Disk implementation of {@link IFileStore} with AES-256 encryption at rest (Component C12).
 * <p>
 * Specifications:
 * <ul>
 *   <li>SAD Table 3.4b & Section 4.3.1 / 4.4</li>
 *   <li>INS-SR-004: Encryption at rest using AES-256-GCM.</li>
 *   <li>INS-SR-007 & STP TC-Sec-07: Maximum 5 MB upload limit (FILE-413), MIME and magic-bytes whitelist (FILE-415), path traversal defense.</li>
 * </ul>
 */
@Service
public class DiskFileStore implements IFileStore {

    private static final Logger log = LoggerFactory.getLogger(DiskFileStore.class);

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L; // 5 MB

    public static final String DEFAULT_DEV_KEY = "0123456789abcdef0123456789abcdef"; // 32 bytes (256-bit)
    public static final String DEFAULT_COMPOSE_HEX_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; // 64-char hex dev key
    public static final String DEFAULT_DEV_KEY_FALLBACK = "dev-aes256-key-team11-workflow!!"; // 32 bytes UTF-8

    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final Path rootPath;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final Map<Long, StoredDocument> indexById = new ConcurrentHashMap<>();
    private final Map<Long, Path> fileLocationById = new ConcurrentHashMap<>();
    private final Map<Long, Path> metaLocationById = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(100);

    @Autowired
    public DiskFileStore(
            @Value("${insurance.filestore.root-dir:${FILE_STORE_DIR:./storage/uploads}}") String rootDir,
            @Value("${insurance.security.encryption-key:}") String encryptionKey,
            Environment environment,
            ObjectProvider<Clock> clockProvider,
            ObjectMapper objectMapper) {

        this(rootDir, encryptionKey, environment,
                clockProvider != null ? clockProvider.getIfAvailable(Clock::systemDefaultZone) : Clock.systemDefaultZone(),
                objectMapper);
    }

    public DiskFileStore(String rootDir, String encryptionKey, Environment environment, Clock clock, ObjectMapper objectMapper) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.clock = clock != null ? clock : Clock.systemDefaultZone();

        byte[] rawKey = resolveKeyBytes(encryptionKey, environment);
        this.secretKey = new SecretKeySpec(rawKey, "AES");
    }

    public DiskFileStore(String rootDir, String encryptionKey, ObjectMapper objectMapper) {
        this(rootDir, encryptionKey, null, Clock.systemDefaultZone(), objectMapper);
    }

    private byte[] resolveKeyBytes(String keyString, Environment env) {
        boolean isDevOrTest = false;
        if (env != null) {
            String[] activeProfiles = env.getActiveProfiles();
            isDevOrTest = Arrays.stream(activeProfiles)
                    .anyMatch(p -> "dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p));
        }

        if (keyString != null && !keyString.trim().isEmpty()) {
            String trimmed = keyString.trim();

            if (!isDevOrTest && (DEFAULT_COMPOSE_HEX_KEY.equalsIgnoreCase(trimmed) || DEFAULT_DEV_KEY_FALLBACK.equals(trimmed))) {
                throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production requires a secure 256-bit AES encryption key! Default dev key cannot be used in production.");
            }

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
            }

            // 3. Fall back to raw UTF-8 32 bytes
            byte[] utf8 = trimmed.getBytes(StandardCharsets.UTF_8);
            if (utf8.length == 32) {
                return utf8;
            }
            throw new IllegalStateException("Configured encryption key ('insurance.security.encryption-key') must be exactly 32 bytes (256-bit) as 64-character hex, Base64, or UTF-8");
        }

        // Key is missing/empty:
        if (isDevOrTest) {
            log.warn("DiskFileStore: No encryption key configured. Falling back to default development key for dev/test profile.");
            return DEFAULT_DEV_KEY.getBytes(StandardCharsets.UTF_8);
        }

        throw new IllegalStateException("CRITICAL SECURITY VIOLATION: Production requires a 256-bit AES encryption key configured via 'insurance.security.encryption-key'!");
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

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootPath);
            for (DocumentCategory category : DocumentCategory.values()) {
                Files.createDirectories(getCategoryDir(category));
            }
            log.info("DiskFileStore initialized at root: {}", rootPath);
            restoreIndexFromDisk();
        } catch (Exception e) {
            throw new FileStorageException("Failed to initialize DiskFileStore directory hierarchy", e);
        }
    }

    private void restoreIndexFromDisk() {
        try {
            if (!Files.exists(rootPath)) {
                return;
            }
            try (Stream<Path> stream = Files.walk(rootPath)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".meta")).forEach(metaPath -> {
                    try {
                        byte[] metaBytes = Files.readAllBytes(metaPath);
                        StoredDocument doc = objectMapper.readValue(metaBytes, StoredDocument.class);
                        if (doc != null && doc.id() != null) {
                            indexById.put(doc.id(), doc);
                            metaLocationById.put(doc.id(), metaPath);

                            String encFileName = metaPath.getFileName().toString().replace(".meta", ".enc");
                            Path encPath = metaPath.getParent().resolve(encFileName);
                            fileLocationById.put(doc.id(), encPath);

                            idSequence.accumulateAndGet(doc.id(), Math::max);
                        }
                    } catch (Exception ex) {
                        log.warn("DiskFileStore: Could not restore metadata from {}", metaPath, ex);
                    }
                });
            }
            log.info("DiskFileStore: Restored {} documents from disk.", indexById.size());
        } catch (Exception ex) {
            log.warn("DiskFileStore: Index restoration encountered warning: {}", ex.getMessage());
        }
    }

    @Override
    public Long put(MultipartFile file, DocumentCategory category, CurrentUser currentUser) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException(ErrorCode.VALID_422, "Uploaded file cannot be null or empty");
        }

        long size = file.getSize();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        try (InputStream is = file.getInputStream()) {
            Long ownerId = currentUser != null ? currentUser.getUserId() : null;
            StoredDocument stored = storeStream(is, originalFilename, contentType, size, category, ownerId);
            return stored.id();
        } catch (FileValidationException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to store uploaded file: " + originalFilename, e);
        }
    }

    @Override
    public StoredDocument storeStream(
            InputStream content,
            String originalFilename,
            String contentType,
            long sizeBytes,
            DocumentCategory category,
            Long ownerUserId) {

        DocumentCategory targetCategory = category != null ? category : DocumentCategory.POLICY;

        // 1. Path traversal defense on original filename
        validateFilenameSafety(originalFilename);

        // 2. Upload size enforcement (INS-SR-007, STP TC-Sec-07: max 5 MB -> HTTP 413)
        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException(ErrorCode.FILE_413,
                    String.format("Uploaded file exceeds maximum allowed limit of 5 MB (size: %d bytes)", sizeBytes));
        }

        // Read bytes to inspect magic header and encrypt
        byte[] rawBytes;
        try {
            rawBytes = content.readAllBytes();
        } catch (Exception e) {
            throw new FileStorageException("Failed to read input stream for: " + originalFilename, e);
        }

        if (rawBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException(ErrorCode.FILE_413,
                    String.format("Uploaded file exceeds maximum allowed limit of 5 MB (size: %d bytes)", rawBytes.length));
        }

        // 3. MIME and magic byte validation (STP TC-Sec-07 -> HTTP 415)
        String validatedContentType = validateFileTypeAndMagicBytes(originalFilename, contentType, rawBytes);

        // 4. Compute SHA-256 checksum of plaintext
        String checksum = computeSha256(rawBytes);

        // 5. Encrypt at rest using AES-256-GCM (INS-SR-004)
        byte[] encryptedBytes;
        try {
            encryptedBytes = encryptPayload(rawBytes);
        } catch (Exception e) {
            throw new FileStorageException("Failed to encrypt document at rest: " + originalFilename, e);
        }

        // 6. Generate unique storage key and write to category sub-directory
        Long docId = idSequence.incrementAndGet();
        String uuid = UUID.randomUUID().toString();
        String storageKey = uuid + ".enc";
        String metaKey = uuid + ".meta";

        Path categoryDir = getCategoryDir(targetCategory);
        Path targetEncFile = categoryDir.resolve(storageKey).normalize();
        Path targetMetaFile = categoryDir.resolve(metaKey).normalize();

        // Path containment check
        if (!targetEncFile.startsWith(rootPath) || !targetMetaFile.startsWith(rootPath)) {
            throw new SecurityException("Path traversal violation: Target file falls outside storage root.");
        }

        StoredDocument storedDocument = new StoredDocument(
                docId,
                storageKey,
                originalFilename,
                validatedContentType,
                rawBytes.length,
                targetCategory,
                checksum,
                ownerUserId,
                clock.instant()
        );

        try {
            Files.write(targetEncFile, encryptedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            byte[] metaJson = objectMapper.writeValueAsBytes(storedDocument);
            Files.write(targetMetaFile, metaJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            indexById.put(docId, storedDocument);
            fileLocationById.put(docId, targetEncFile);
            metaLocationById.put(docId, targetMetaFile);

            log.info("DiskFileStore: Stored document ID={} (originalFilename='{}', category={}, size={} bytes, encryptedSize={} bytes)",
                    docId, originalFilename, targetCategory, rawBytes.length, encryptedBytes.length);

            return storedDocument;
        } catch (Exception e) {
            try {
                Files.deleteIfExists(targetEncFile);
                Files.deleteIfExists(targetMetaFile);
            } catch (Exception ignored) {
            }
            throw new FileStorageException("Failed to write encrypted document to disk", e);
        }
    }

    @Override
    public LoadedDocument get(Long id, CurrentUser currentUser) {
        if (id == null) {
            throw new ResourceNotFoundException("Document", "null");
        }

        StoredDocument metadata = indexById.get(id);
        Path encPath = fileLocationById.get(id);

        if (metadata == null || encPath == null || !Files.exists(encPath)) {
            throw new ResourceNotFoundException("Document", id);
        }

        // Authorization check fails closed if currentUser is null or unauthorized
        checkAuthorization(metadata, currentUser);

        try {
            byte[] encryptedBytes = Files.readAllBytes(encPath);
            byte[] decryptedBytes = decryptPayload(encryptedBytes);

            ByteArrayResource resource = new ByteArrayResource(decryptedBytes, metadata.originalFilename());
            return new LoadedDocument(metadata, resource);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Failed to decrypt or read document with ID: " + id, e);
        }
    }

    @Override
    public void delete(Long id, CurrentUser currentUser) {
        if (id == null) {
            throw new ResourceNotFoundException("Document", "null");
        }

        StoredDocument metadata = indexById.get(id);
        Path encPath = fileLocationById.get(id);
        Path metaPath = metaLocationById.get(id);

        if (metadata == null || encPath == null || !Files.exists(encPath)) {
            throw new ResourceNotFoundException("Document", id);
        }

        // Authorization check fails closed if currentUser is null or unauthorized
        checkAuthorization(metadata, currentUser);

        try {
            Files.deleteIfExists(encPath);
            if (metaPath != null) {
                Files.deleteIfExists(metaPath);
            }
            indexById.remove(id);
            fileLocationById.remove(id);
            metaLocationById.remove(id);
            log.info("DiskFileStore: Deleted document ID={}", id);
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete document file with ID: " + id, e);
        }
    }

    /**
     * Explicit internal trusted path for system/background workflows that execute outside an active user request.
     */
    public LoadedDocument getInternal(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Document", "null");
        }

        StoredDocument metadata = indexById.get(id);
        Path encPath = fileLocationById.get(id);

        if (metadata == null || encPath == null || !Files.exists(encPath)) {
            throw new ResourceNotFoundException("Document", id);
        }

        try {
            byte[] encryptedBytes = Files.readAllBytes(encPath);
            byte[] decryptedBytes = decryptPayload(encryptedBytes);

            ByteArrayResource resource = new ByteArrayResource(decryptedBytes, metadata.originalFilename());
            return new LoadedDocument(metadata, resource);
        } catch (Exception e) {
            throw new FileStorageException("Failed to decrypt or read document with ID: " + id, e);
        }
    }

    /**
     * Explicit internal trusted path for system cleanup workflows that execute outside an active user request.
     */
    public void deleteInternal(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("Document", "null");
        }

        StoredDocument metadata = indexById.get(id);
        Path encPath = fileLocationById.get(id);
        Path metaPath = metaLocationById.get(id);

        if (metadata == null || encPath == null || !Files.exists(encPath)) {
            throw new ResourceNotFoundException("Document", id);
        }

        try {
            Files.deleteIfExists(encPath);
            if (metaPath != null) {
                Files.deleteIfExists(metaPath);
            }
            indexById.remove(id);
            fileLocationById.remove(id);
            metaLocationById.remove(id);
            log.info("DiskFileStore: Deleted document internally ID={}", id);
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete document file with ID: " + id, e);
        }
    }

    @Override
    public boolean exists(Long id) {
        if (id == null) {
            return false;
        }
        Path path = fileLocationById.get(id);
        return path != null && Files.exists(path);
    }

    // =========================================================================
    // Security & Cryptographic Helpers
    // =========================================================================

    private void checkAuthorization(StoredDocument doc, CurrentUser user) {
        if (user == null) {
            throw new AccessDeniedException("Unauthenticated request: user context is required to access document " + doc.id());
        }
        // System admin has full access
        if (user.isSystemAdmin()) {
            return;
        }
        // Insurer staff (underwriter, claims officer, insurer admin) can access documents
        if (user.isStaff()) {
            return;
        }
        // Customer can only access their own documents
        if (doc.ownerUserId() != null && Objects.equals(doc.ownerUserId(), user.getUserId())) {
            return;
        }
        throw new AccessDeniedException("User " + user.getUserId() + " is not authorized to access document " + doc.id());
    }

    private void validateFilenameSafety(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileValidationException(ErrorCode.VALID_422, "Filename cannot be empty");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.indexOf('\0') >= 0) {
            throw new FileValidationException(ErrorCode.VALID_422, "Illegal filename or path traversal detected: " + filename);
        }
    }

    private String validateFileTypeAndMagicBytes(String filename, String declaredContentType, byte[] bytes) {
        String lower = filename.toLowerCase();
        boolean hasAllowedExt = lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");

        if (!hasAllowedExt) {
            throw new FileValidationException(ErrorCode.FILE_415,
                    "Disallowed file extension. Only PDF, JPG, and PNG documents are allowed (STP TC-Sec-07).");
        }

        // Verify magic bytes
        if (bytes == null || bytes.length < 4) {
            throw new FileValidationException(ErrorCode.FILE_415, "File content is empty or too short to identify valid format.");
        }

        // PDF: starts with %PDF- (0x25 0x50 0x44 0x46 0x2D)
        if (lower.endsWith(".pdf")) {
            if (bytes.length >= 5 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D) {
                return "application/pdf";
            }
            throw new FileValidationException(ErrorCode.FILE_415, "Disguised file: file claims to be PDF but lacks valid '%PDF-' header.");
        }

        // JPEG: starts with FF D8 FF
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            throw new FileValidationException(ErrorCode.FILE_415, "Disguised file: file claims to be JPEG but lacks valid SOI marker.");
        }

        // PNG: starts with 89 50 4E 47 (0x89 'P' 'N' 'G')
        if (lower.endsWith(".png")) {
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            throw new FileValidationException(ErrorCode.FILE_415, "Disguised file: file claims to be PNG but lacks valid signature.");
        }

        throw new FileValidationException(ErrorCode.FILE_415, "Disallowed media type.");
    }

    private byte[] encryptPayload(byte[] plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] cipherText = cipher.doFinal(plaintext);

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return combined;
    }

    private byte[] decryptPayload(byte[] combined) throws Exception {
        if (combined.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw new SecurityException("Ciphertext payload is truncated or corrupted");
        }
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);

        int cipherLen = combined.length - GCM_IV_LENGTH_BYTES;
        byte[] cipherText = new byte[cipherLen];
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherLen);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(cipherText);
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Path getCategoryDir(DocumentCategory category) {
        String sub = switch (category) {
            case KYC -> "kyc";
            case POLICY -> "policies";
            case CLAIM -> "claims";
        };
        return rootPath.resolve(sub);
    }

    public Path getRootPath() {
        return rootPath;
    }

    public void clearAll() {
        indexById.clear();
        fileLocationById.clear();
        metaLocationById.clear();
    }
}
