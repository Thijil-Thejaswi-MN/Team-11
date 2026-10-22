package com.team11.insurance.filestore.simulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Controllable simulator for PDF document generation and file storage I/O failures (P4-05).
 * <p>
 * Supports deterministic simulation of:
 * <ul>
 *   <li>SUCCESS: generates valid PDF binary headers and dummy content.</li>
 *   <li>IO_FAILURE: simulates disk write error or memory exhausted I/O failure.</li>
 * </ul>
 */
@Component
@Profile({"dev", "test"})
public class PdfGeneratorSimulator {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorSimulator.class);

    public enum GenerationMode {
        SUCCESS,
        IO_FAILURE
    }

    private volatile GenerationMode mode = GenerationMode.SUCCESS;
    private final AtomicInteger generatedCount = new AtomicInteger(0);

    public void setMode(GenerationMode mode) {
        this.mode = mode;
        log.info("PdfGeneratorSimulator mode set to: {}", mode);
    }

    public void reset() {
        this.mode = GenerationMode.SUCCESS;
        this.generatedCount.set(0);
        log.info("PdfGeneratorSimulator reset to SUCCESS with cleared counter.");
    }

    public int getGeneratedCount() {
        return generatedCount.get();
    }

    /**
     * Generates a valid PDF byte array or throws an IOException based on simulated mode.
     *
     * @param documentTitle title or identifier for the document
     * @param metadata      metadata fields to embed in the simulated PDF
     * @return PDF byte array with valid magic-byte header (%PDF-)
     * @throws IOException if mode is IO_FAILURE
     */
    public byte[] generatePdf(String documentTitle, Map<String, Object> metadata) throws IOException {
        if (mode == GenerationMode.IO_FAILURE) {
            log.error("PdfGeneratorSimulator: simulating I/O failure during PDF generation for {}", documentTitle);
            throw new IOException("Simulated disk I/O failure or corrupted output stream while rendering PDF: " + documentTitle);
        }

        generatedCount.incrementAndGet();
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("%Simulated Insurance Certificate PDF\n");
        sb.append("Title: ").append(documentTitle != null ? documentTitle : "Insurance Document").append("\n");
        if (metadata != null) {
            metadata.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        }
        sb.append("%%EOF\n");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        log.info("PdfGeneratorSimulator: successfully generated PDF for [{}] (size={} bytes)", documentTitle, bytes.length);
        return bytes;
    }
}
