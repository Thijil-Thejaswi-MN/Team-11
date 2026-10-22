package com.team11.insurance.payments;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.payments.dto.PaymentInitiationRequest;
import com.team11.insurance.payments.simulator.PaymentGatewaySimulator;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P4-17 PaymentController REST Integration Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationHistoryRepository applicationHistoryRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PaymentGatewaySimulator paymentGatewaySimulator;

    @Autowired
    private PaymentServiceStub paymentServiceStub;

    private Application testApp;
    private Quote testQuote;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.clearSyntheticUser();
        policyRepository.deleteAll();
        applicationHistoryRepository.deleteAll();
        applicationRepository.deleteAll();
        quoteRepository.deleteAll();
        paymentServiceStub.reset();
        paymentGatewaySimulator.reset();

        testQuote = new Quote();
        testQuote.setCustomerId(1001L);
        testQuote.setInsurerId(1L);
        testQuote.setPlanId(10L);
        testQuote.setProductType("FOUR_WHEELER");
        testQuote.setPremium(new BigDecimal("8450.00"));
        testQuote = quoteRepository.save(testQuote);

        testApp = new Application();
        testApp.setCustomerId(1001L);
        testApp.setInsurerId(1L);
        testApp.setPlanId(10L);
        testApp.setQuoteId(testQuote.getId());
        testApp.setProductType("FOUR_WHEELER");
        testApp.setVehicleRegNo("KA22AB1234");
        testApp.setPremium(new BigDecimal("8450.00"));
        testApp.setStatus(ApplicationStatus.APPROVED);
        testApp = applicationRepository.save(testApp);

        // Default test customer matches testApp customerId
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(1001L, UserRole.CUSTOMER, null));
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    @Test
    @DisplayName("POST /payments: Success 200 returns paymentStatus=Success, policyNo and derived amount")
    void testPostPayment_Success() throws Exception {
        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("Success"))
                .andExpect(jsonPath("$.policyNo", startsWith("POL-")))
                .andExpect(jsonPath("$.amount").value(8450.00));
    }

    @Test
    @DisplayName("POST /payments: Failed gateway returns HTTP 402 with PAY-402 errorCode and retryable message")
    void testPostPayment_402_PaymentFailed() throws Exception {
        paymentGatewaySimulator.setMode(PaymentGatewaySimulator.SimulatorMode.DECLINED);

        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.errorCode").value("PAY-402"))
                .andExpect(jsonPath("$.message", containsString("declined")));
    }

    @Test
    @DisplayName("POST /payments: Non-approved application returns HTTP 409 with STATE-409 errorCode")
    void testPostPayment_409_Conflict_NotApproved() throws Exception {
        testApp.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(testApp);

        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("STATE-409"))
                .andExpect(jsonPath("$.message", containsString("not approved")));
    }

    @Test
    @DisplayName("POST /payments: Already paid application returns HTTP 409 with STATE-409 errorCode")
    void testPostPayment_409_Conflict_AlreadyPaid() throws Exception {
        testApp.setStatus(ApplicationStatus.PAID);
        applicationRepository.save(testApp);

        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("STATE-409"))
                .andExpect(jsonPath("$.message", containsString("already been paid")));
    }

    @Test
    @DisplayName("POST /payments: Different customer gets HTTP 403 ACCESS-403")
    void testPostPayment_403_AccessDenied() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(new CurrentUser(9999L, UserRole.CUSTOMER, null));

        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
    }

    @Test
    @DisplayName("POST /payments: Missing or invalid applicationId returns HTTP 400")
    void testPostPayment_400_Validation() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicationId\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /payments/applications/{id}: Returns 200 with application summary")
    void testGetApplicationSummary() throws Exception {
        mockMvc.perform(get("/payments/applications/" + testApp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(testApp.getId()))
                .andExpect(jsonPath("$.premium").value(8450.00))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /payments: Unauthenticated user returns HTTP 401 AUTH-401")
    void testPostPayment_Unauthenticated_Returns401() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        PaymentInitiationRequest request = new PaymentInitiationRequest(testApp.getId());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }

    @Test
    @DisplayName("GET /payments/applications/{id}: Unauthenticated user returns HTTP 401 AUTH-401")
    void testGetApplicationSummary_Unauthenticated_Returns401() throws Exception {
        TestCurrentUserProvider.setUnauthenticated();

        mockMvc.perform(get("/payments/applications/" + testApp.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTH-401"));
    }
}
