package com.team11.insurance.filestore.simulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("P4-05 PdfGeneratorSimulator Unit Tests")
class PdfGenerationFailureSimulatorTest {

    private PdfGeneratorSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new PdfGeneratorSimulator();
    }

    @Test
    @DisplayName("SUCCESS mode generates valid PDF bytes with %PDF- header and %%EOF footer")
    void testSuccessMode() throws Exception {
        byte[] pdf = simulator.generatePdf("POL-2026-000101", Map.of("policyNo", "POL-2026-000101", "premium", "8500.00"));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String content = new String(pdf, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("%PDF-"), "Must have standard PDF magic bytes header");
        assertTrue(content.contains("POL-2026-000101"));
        assertTrue(content.contains("8500.00"));
        assertTrue(content.endsWith("%%EOF\n"));
        assertEquals(1, simulator.getGeneratedCount());
    }

    @Test
    @DisplayName("IO_FAILURE mode throws IOException to simulate disk write failure")
    void testIoFailureMode() {
        simulator.setMode(PdfGeneratorSimulator.GenerationMode.IO_FAILURE);

        IOException ex = assertThrows(IOException.class, () ->
                simulator.generatePdf("POL-2026-FAIL", Map.of()));

        assertTrue(ex.getMessage().contains("Simulated disk I/O failure"));
        assertEquals(0, simulator.getGeneratedCount());
    }

    @Test
    @DisplayName("Resetting simulator clears counters and restores SUCCESS mode")
    void testReset() throws Exception {
        simulator.setMode(PdfGeneratorSimulator.GenerationMode.IO_FAILURE);
        simulator.reset();

        byte[] pdf = simulator.generatePdf("POL-RESET", Map.of());
        assertNotNull(pdf);
        assertEquals(1, simulator.getGeneratedCount());
    }
}
