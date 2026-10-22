package com.team11.insurance.filestore;

import org.springframework.core.io.Resource;

/**
 * Result record containing document metadata and streamable Resource (SAD Table 3.4b).
 */
public record LoadedDocument(
    StoredDocument metadata,
    Resource resource
) { }
