package com.team11.insurance.filestore;

import java.time.Instant;

/**
 * Metadata record for a stored file document (SAD Table 3.4b, §4.3.1).
 */
public record StoredDocument(
    Long id,                       // Relational PK (Long) - stored in Policy.documentFileId
    String storageKey,             // UUID on disk (e.g. "a1b2c3d4-e5f6-7890.pdf")
    String originalFilename,       // User display filename
    String contentType,            // "application/pdf", "image/jpeg", "image/png"
    long sizeBytes,                // <= 5 * 1024 * 1024
    DocumentCategory category,
    String sha256Checksum,
    Long ownerUserId,
    Instant uploadedAt
) { }
