package com.team11.insurance.filestore;

import org.springframework.core.io.Resource;

/**
 * Encapsulates retrieved document metadata alongside its readable Resource stream (Component C12 - P4-03 Contract Section 6.2).
 */
public record LoadedDocument(
        StoredDocument metadata,
        Resource resource
) { }
