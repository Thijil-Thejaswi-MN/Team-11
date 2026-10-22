package com.team11.insurance.filestore;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import com.team11.insurance.auth.CurrentUser;

/**
 * Encrypted Document Store Interface (Component C12 - SAD Table 3.4b, INS-SR-004, INS-SR-007).
 * Consumed by: User & KYC (C3), Quote & Purchase (C5), Claims Workflow (C7).
 */
public interface IFileStore {

    /**
     * Stores an uploaded file and returns document ID (SAD Table 3.4b put(file) returns id).
     * Enforces size <= 5MB (HTTP 413), MIME allowlist (HTTP 415), and path traversal protection.
     *
     * @param file        the incoming multipart file
     * @param category    storage category (KYC, POLICY, CLAIM)
     * @param currentUser authenticated user for ownership tagging
     * @return unique database ID (Long) matching Policy.documentFileId
     */
    Long put(MultipartFile file, DocumentCategory category, CurrentUser currentUser);

    /**
     * Retrieves stored document by database ID (SAD Table 3.4b get(id)).
     * Enforces ownership or insurer-staff authorization.
     *
     * @param id          document database ID
     * @param currentUser requesting user
     * @return LoadedDocument containing metadata and Spring Resource stream
     */
    LoadedDocument get(Long id, CurrentUser currentUser);

    /**
     * Deletes stored document by database ID (SAD Table 3.4b delete(id)).
     */
    void delete(Long id, CurrentUser currentUser);

    /**
     * Verifies existence of document by database ID.
     */
    boolean exists(Long id);

    // =========================================================================
    // Compatibility Helpers (Supported adapters for streaming and full metadata)
    // =========================================================================
    StoredDocument storeStream(InputStream content, String originalFilename, String contentType,
                               long sizeBytes, DocumentCategory category, Long ownerUserId);

    default LoadedDocument loadFile(Long id, CurrentUser user) {
        return get(id, user);
    }

    default void deleteFile(Long id, CurrentUser user) {
        delete(id, user);
    }
}
