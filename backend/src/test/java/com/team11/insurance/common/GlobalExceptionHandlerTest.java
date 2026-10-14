package com.team11.insurance.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.common.logging.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration and WebMvc tests verifying GlobalExceptionHandler compliance
 * with the standard error response schema in SAD Section 4.4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Validation Error returns status 400, errorCode VALID-422, field details, and correlationId")
    void testValidationFailureReturnsValid422() throws Exception {
        // Missing applicantName, invalid email, negative amount
        Map<String, Object> invalidPayload = Map.of(
                "applicantName", "",
                "email", "not-a-valid-email",
                "declaredAmount", new BigDecimal("-500")
        );

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload))
                        .header(CorrelationIdFilter.HEADER_NAME, "corr-test-12345"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "corr-test-12345"))
                // Standard SAD 4.4 Schema assertions
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message").value("Input failed validation or a business rule"))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.correlationId").value("corr-test-12345"));
    }

    @Test
    @DisplayName("ResourceNotFoundException returns status 404, errorCode NOTFOUND-404")
    void testResourceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .header(CorrelationIdFilter.HEADER_NAME, "corr-notfound-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("NOTFOUND-404"))
                .andExpect(jsonPath("$.message", containsString("Policy with identifier 'POL-2026-999' was not found")))
                .andExpect(jsonPath("$.correlationId").value("corr-notfound-001"));
    }

    @Test
    @DisplayName("StateConflictException returns status 409, errorCode STATE-409")
    void testStateConflictReturns409() throws Exception {
        mockMvc.perform(get("/test/state-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("STATE-409"))
                .andExpect(jsonPath("$.message", containsString("CANCELLED state")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @DisplayName("PaymentFailedException returns status 402, errorCode PAY-402")
    void testPaymentFailedReturns402() throws Exception {
        mockMvc.perform(get("/test/payment-failed"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.errorCode").value("PAY-402"))
                .andExpect(jsonPath("$.message", containsString("retried without double charging")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @DisplayName("AccessDeniedException returns status 403, errorCode ACCESS-403")
    void testAccessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"))
                .andExpect(jsonPath("$.message", containsString("INS-SR-004")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @DisplayName("AccountLockedException returns status 423, errorCode AUTH-423")
    void testAccountLockedReturns423() throws Exception {
        mockMvc.perform(get("/test/account-locked"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.errorCode").value("AUTH-423"))
                .andExpect(jsonPath("$.message", containsString("locked after 5 failed login attempts")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @DisplayName("AuthenticationException returns status 401, errorCode AUTH-401")
    void testUnauthorizedReturns401() throws Exception {
        mockMvc.perform(get("/test/auth-unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"))
                .andExpect(jsonPath("$.message", containsString("15 minutes")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @DisplayName("Unexpected Exception returns status 500, errorCode SYS-500, without leaking sensitive internals")
    void testUnexpectedExceptionReturns500WithoutLeak() throws Exception {
        mockMvc.perform(get("/test/system-error")
                        .header(CorrelationIdFilter.HEADER_NAME, "corr-sys-error-888"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "corr-sys-error-888"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("SYS-500"))
                .andExpect(jsonPath("$.message", containsString("unexpected internal error occurred")))
                // Ensure internal stack trace or secret password is NEVER leaked to client
                .andExpect(jsonPath("$.message", not(containsString("SecretPassword"))))
                .andExpect(jsonPath("$.details", hasSize(0)))
                .andExpect(jsonPath("$.correlationId").value("corr-sys-error-888"));
    }
}
