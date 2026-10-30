package com.team11.insurance.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.TestCurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for GET /audit (INS-F-018, INS-SR-008, TC-Aud-01, TC-Sec-08).
 * Verifies RBAC (SYSTEM_ADMIN only; 403 for other roles, 401 for unauthenticated),
 * filtering (userId, action, entity, entityId, from, to), pagination, PII masking,
 * and rejection of mutating HTTP verbs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P2-18 / GET /audit Controller Integration Tests (TC-Aud-01, TC-Sec-08)")
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private IAuditLog auditLogService;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_SYSTEM_ADMIN);
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("SYSTEM_ADMIN accesses GET /audit successfully (200 OK)")
    void systemAdminAccessSuccess() throws Exception {
        auditLogService.record(5001L, "SYSTEM_CONFIG_UPDATED", "Config", 101L, "System parameter tuned");

        mockMvc.perform(get("/audit")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /audit/logs alias also functions for backward compatibility")
    void auditLogsAliasSuccess() throws Exception {
        mockMvc.perform(get("/audit/logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
    }

    @Test
    @DisplayName("CUSTOMER role is denied GET /audit with 403 Forbidden")
    void customerRoleForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS-403")));
    }

    @Test
    @DisplayName("INSURER_ADMIN role is denied GET /audit with 403 Forbidden")
    void insurerAdminRoleForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN);

        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS-403")));
    }

    @Test
    @DisplayName("UNDERWRITER role is denied GET /audit with 403 Forbidden")
    void underwriterRoleForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS-403")));
    }

    @Test
    @DisplayName("CLAIMS_OFFICER role is denied GET /audit with 403 Forbidden")
    void claimsOfficerRoleForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CLAIMS_OFFICER);

        mockMvc.perform(get("/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS-403")));
    }

    @Test
    @DisplayName("Unauthenticated request is denied with 401 Unauthorized")
    void unauthenticatedDenied() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("AUTH-401")));
    }

    @Test
    @DisplayName("Filter by userId returns only matched entries")
    void filterByUserId() throws Exception {
        long targetUser = 888123L;
        auditLogService.record(targetUser, "PASSWORD_RESET", "User", targetUser, "Password changed");

        mockMvc.perform(get("/audit")
                        .param("userId", String.valueOf(targetUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[0].userId", is((int) targetUser)));
    }

    @Test
    @DisplayName("Filter by action code returns only matching actions")
    void filterByAction() throws Exception {
        String uniqueAction = "CUSTOM_AUDIT_ACTION_TEST";
        auditLogService.record(5001L, uniqueAction, "Policy", 9991L, "Action specific audit");

        mockMvc.perform(get("/audit")
                        .param("action", uniqueAction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[0].action", is(uniqueAction)));
    }

    @Test
    @DisplayName("Filter by entity and entityId")
    void filterByEntityAndEntityId() throws Exception {
        String entity = "Claim";
        long entityId = 771122L;
        auditLogService.record(5001L, "CLAIM_ASSESSED", entity, entityId, "Assessment completed");

        mockMvc.perform(get("/audit")
                        .param("entity", entity)
                        .param("entityId", String.valueOf(entityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.items[0].entity", is(entity)))
                .andExpect(jsonPath("$.items[0].entityId", is((int) entityId)));
    }

    @Test
    @DisplayName("Filter by ISO date range (from and to)")
    void filterByDateRange() throws Exception {
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant to = Instant.now().plus(2, ChronoUnit.HOURS);

        auditLogService.record(5001L, "DATE_RANGE_EVENT", "System", 1L, "Time window test");

        mockMvc.perform(get("/audit")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", notNullValue()));
    }

    @Test
    @DisplayName("Invalid filter: malformed 'from' date returns 400/422 Bad Request")
    void invalidFromDateReturns400() throws Exception {
        mockMvc.perform(get("/audit")
                        .param("from", "invalid-date-string"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")));
    }

    @Test
    @DisplayName("Invalid filter: 'from' after 'to' returns 400/422 Bad Request")
    void fromAfterToReturns400() throws Exception {
        mockMvc.perform(get("/audit")
                        .param("from", "2026-10-28T12:00:00Z")
                        .param("to", "2026-10-27T12:00:00Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")));
    }

    @Test
    @DisplayName("Invalid filter: negative page or zero size returns 400/422 Bad Request")
    void invalidPaginationReturns400() throws Exception {
        mockMvc.perform(get("/audit")
                        .param("page", "-1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")));

        mockMvc.perform(get("/audit")
                        .param("size", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")));

        mockMvc.perform(get("/audit")
                        .param("size", "500"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", is("VALID-422")));
    }

    @Test
    @DisplayName("Sensitive data (PAN, Aadhaar, password) in details is masked in GET /audit response")
    void sensitiveDataScrubbedInDetails() throws Exception {
        String sensitiveDetails = "Customer PAN is ABCDE1234F, Aadhaar is 123456789012, and password: \"Secret123!\"";
        auditLogService.record(5001L, "CUSTOMER_PII_LOG", "Customer", 601L, sensitiveDetails);

        mockMvc.perform(get("/audit")
                        .param("action", "CUSTOMER_PII_LOG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].details", not(containsString("ABCDE1234F"))))
                .andExpect(jsonPath("$.items[0].details", not(containsString("123456789012"))))
                .andExpect(jsonPath("$.items[0].details", not(containsString("Secret123!"))))
                .andExpect(jsonPath("$.items[0].details", containsString("ABCDE****F")))
                .andExpect(jsonPath("$.items[0].details", containsString("********9012")));
    }

    @Test
    @DisplayName("AuditLogController exposes NO mutating HTTP methods (POST, PUT, PATCH, DELETE)")
    void noMutatingMethodsExposed() throws Exception {
        mockMvc.perform(post("/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/audit"))
                .andExpect(status().isMethodNotAllowed());
    }
}
