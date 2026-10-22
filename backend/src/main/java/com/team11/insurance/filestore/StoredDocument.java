package com.team11.insurance.filestore;

import java.time.Instant;

/**
 * Metadata record for a stored document (Component C12 - SAD Table 3.4b, INS-SR-004, INS-SR-007, P4-03 Contract Section 6.2).
 */
public record StoredDocument(
        Long id,
        String storageKey,
        String originalFilename,
        String contentType,
        long sizeBytes,
        DocumentCategory category,
        String sha256Checksum,
        Long ownerUserId,
        Instant uploadedAt
) { }
