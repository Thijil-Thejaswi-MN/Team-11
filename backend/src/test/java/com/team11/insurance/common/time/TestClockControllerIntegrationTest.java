package com.team11.insurance.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P4-05 TestClockController REST Integration Tests")
class TestClockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableTestClock clock;

    @Test
    @DisplayName("GET /api/v1/test/clock returns current virtual time and status SUCCESS")
    void testGetVirtualTime() throws Exception {
        mockMvc.perform(get("/test/clock")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.action", is("GET")))
                .andExpect(jsonPath("$.virtualInstant", notNullValue()))
                .andExpect(jsonPath("$.virtualDate", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/test/clock with SET sets fixed virtual instant")
    void testSetFixedInstant() throws Exception {
        String payload = """
                {
                  "action": "SET",
                  "isoInstant": "2026-11-01T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/test/clock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.action", is("SET")))
                .andExpect(jsonPath("$.virtualInstant", is("2026-11-01T10:00:00Z")))
                .andExpect(jsonPath("$.offsetSeconds", is(0)));
    }

    @Test
    @DisplayName("POST /api/v1/test/clock with ADVANCE increments virtual time by specified duration")
    void testAdvanceVirtualTime() throws Exception {
        // First set to known instant
        clock.setFixed(java.time.Instant.parse("2026-10-20T00:00:00Z"));

        String payload = """
                {
                  "action": "ADVANCE",
                  "amount": 15,
                  "unit": "DAYS"
                }
                """;

        mockMvc.perform(post("/test/clock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.action", is("ADVANCE")))
                .andExpect(jsonPath("$.virtualInstant", is("2026-11-04T00:00:00Z")))
                .andExpect(jsonPath("$.offsetSeconds", is(15 * 86400)));
    }

    @Test
    @DisplayName("POST /api/v1/test/clock with RESET reverts virtual clock to system time")
    void testResetVirtualTime() throws Exception {
        String payload = """
                {
                  "action": "RESET"
                }
                """;

        mockMvc.perform(post("/test/clock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.action", is("RESET")))
                .andExpect(jsonPath("$.offsetSeconds", is(0)));
    }
}
