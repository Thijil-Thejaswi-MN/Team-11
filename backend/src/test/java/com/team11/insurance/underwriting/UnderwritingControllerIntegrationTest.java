package com.team11.insurance.underwriting;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.underwriting.dto.UnderwriterDecisionRequest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UnderwritingController REST Integration Tests")
class UnderwritingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Application referredAppInsurer1;
    private Application referredAppInsurer2;

    @BeforeEach
    void setUp() {
        referredAppInsurer1 = new Application();
        referredAppInsurer1.setCustomerId(1001L);
        referredAppInsurer1.setInsurerId(1L);
        referredAppInsurer1.setPlanId(1L);
        referredAppInsurer1.setQuoteId(5001L);
        referredAppInsurer1.setProductType("HEALTH");
        referredAppInsurer1.setPremium(new BigDecimal("9000.00"));
        referredAppInsurer1.setStatus(ApplicationStatus.REFERRED);
        referredAppInsurer1 = applicationRepository.saveAndFlush(referredAppInsurer1);

        referredAppInsurer2 = new Application();
        referredAppInsurer2.setCustomerId(1002L);
        referredAppInsurer2.setInsurerId(2L);
        referredAppInsurer2.setPlanId(2L);
        referredAppInsurer2.setQuoteId(5002L);
        referredAppInsurer2.setProductType("HEALTH");
        referredAppInsurer2.setPremium(new BigDecimal("9500.00"));
        referredAppInsurer2.setStatus(ApplicationStatus.REFERRED);
        referredAppInsurer2 = applicationRepository.saveAndFlush(referredAppInsurer2);
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("GET /applications/referred: Underwriter sees referred DTOs without exposing internal updatedAt")
    void underwriterSeesOnlyOwnInsurerReferredApplications() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER); // Insurer 1

        mockMvc.perform(get("/applications/referred"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(referredAppInsurer1.getId()))
                .andExpect(jsonPath("$[0].insurerId").value(1))
                .andExpect(jsonPath("$[0].productType").value("HEALTH"))
                .andExpect(jsonPath("$[0].status").value("REFERRED"))
                .andExpect(jsonPath("$[0].customerId").value(1001))
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist());
    }

    @Test
    @DisplayName("GET /applications/referred: Sanitizes detailsJson to expose only risk factors, withholding sensitive PII")
    void underwriterQueueSanitizesSensitiveFieldsFromDetailsJson() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER); // Insurer 1

        referredAppInsurer1.setDetailsJson("{\"age\":62,\"preExisting\":true,\"aadhaar\":\"9999-8888-7777\",\"bankAccount\":\"SBIN0001234\",\"pan\":\"ABCDE1234F\"}");
        applicationRepository.saveAndFlush(referredAppInsurer1);

        mockMvc.perform(get("/applications/referred"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].detailsJson").value(org.hamcrest.Matchers.containsString("\"age\":62")))
                .andExpect(jsonPath("$[0].detailsJson").value(org.hamcrest.Matchers.containsString("\"preExisting\":true")))
                .andExpect(jsonPath("$[0].detailsJson").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("aadhaar"))))
                .andExpect(jsonPath("$[0].detailsJson").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bankAccount"))))
                .andExpect(jsonPath("$[0].detailsJson").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("pan"))));
    }

    @Test
    @DisplayName("GET /applications/referred: Customer role is denied (403 ACCESS-403)")
    void customerDeniedReferredQueue() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);

        mockMvc.perform(get("/applications/referred"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("POST /applications/{id}/decision: Underwriter approves referred application (200 OK)")
    void underwriterApprovesReferredApplication() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER); // Insurer 1

        UnderwriterDecisionRequest req = new UnderwriterDecisionRequest(
                Decision.APPROVE,
                "Medical documents verified against underwriting guidelines."
        );

        mockMvc.perform(post("/applications/" + referredAppInsurer1.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(referredAppInsurer1.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reason").value("Medical documents verified against underwriting guidelines."));
    }

    @Test
    @DisplayName("POST /applications/{id}/decision: Underwriter rejects referred application (200 OK)")
    void underwriterRejectsReferredApplication() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER); // Insurer 1

        UnderwriterDecisionRequest req = new UnderwriterDecisionRequest(
                Decision.REJECT,
                "Declined due to uninsurable medical condition."
        );

        mockMvc.perform(post("/applications/" + referredAppInsurer1.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(referredAppInsurer1.getId()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Declined due to uninsurable medical condition."));
    }

    @Test
    @DisplayName("POST /applications/{id}/decision: Underwriter of Insurer 2 denied on Insurer 1 application (403 ACCESS-403)")
    void crossInsurerDecisionDenied() throws Exception {
        // Underwriter from Insurer 2
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(3002L, UserRole.UNDERWRITER, 2L));

        UnderwriterDecisionRequest req = new UnderwriterDecisionRequest(
                Decision.APPROVE,
                "Attempted cross-insurer approval"
        );

        mockMvc.perform(post("/applications/" + referredAppInsurer1.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("POST /applications/{id}/decision: Blank reason rejected with 400 Bad Request")
    void blankReasonValidationFailure() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_UNDERWRITER);

        UnderwriterDecisionRequest req = new UnderwriterDecisionRequest(Decision.APPROVE, "");

        mockMvc.perform(post("/applications/" + referredAppInsurer1.getId() + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
    }
}
