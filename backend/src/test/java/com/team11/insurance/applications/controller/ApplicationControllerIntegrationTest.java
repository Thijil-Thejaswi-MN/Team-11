package com.team11.insurance.applications.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.TestCurrentUserProvider;
import com.team11.insurance.kyc.service.KycService;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;
import com.team11.insurance.underwriting.IUnderwriting;
import com.team11.insurance.underwriting.UnderwritingResult;

/** TC-App-01: POST /applications end to end (real database, real service, stub underwriting). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("P3-04a POST /applications")
class ApplicationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ApplicationHistoryRepository historyRepository;

    /** Replaces the real KYC service so each test decides whether the customer is verified. */
    @MockBean private KycService kycService;

    /** Replaces underwriting engine with test stub behavior. */
    @MockBean private IUnderwriting underwriting;

    private Quote quote;

    @BeforeEach
    void setUp() {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);   // user 1001
        when(kycService.isVerified(1001L)).thenReturn(true);
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.approve("Stub approved"));
        quote = quoteRepository.save(new Quote(1001L, 1L, 1L, "MOTOR_FOUR_WHEELER", new BigDecimal("8640"),
                "{}", "{}", Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    @AfterEach
    void tearDown() {
        TestCurrentUserProvider.clearSyntheticUser();
    }

    private String body(List<Long> documentIds) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "quoteId", quote.getId(),
                "details", Map.of("vehicleRegNo", "KA22AB1234"),
                "documentIds", documentIds));
    }

    private Application savedApplication() {
        return applicationRepository.findAll().stream()
                .filter(a -> quote.getId().equals(a.getQuoteId())).findFirst().orElse(null);
    }

    @Test
    @DisplayName("Approved: 201 and the application is saved Submitted then Approved")
    void approvedReturns201() throws Exception {
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").isNumber())
                .andExpect(jsonPath("$.status").value("Approved"));

        Application saved = savedApplication();
        assertNotNull(saved);
        assertEquals(ApplicationStatus.APPROVED, saved.getStatus());
        assertEquals(1L, saved.getInsurerId());
        assertEquals("KA22AB1234", saved.getVehicleRegNo());
        List<ApplicationHistory> history = historyRepository.findByApplicationIdOrderByIdAsc(saved.getId());
        assertEquals(2, history.size());
        assertEquals(ApplicationStatus.SUBMITTED, history.get(0).getToStatus());
        assertEquals(ApplicationStatus.APPROVED, history.get(1).getToStatus());
    }

    @Test
    @DisplayName("Referred: 201 with status Referred")
    void referredReturns201() throws Exception {
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.refer("IDV above auto limit"));
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Referred"));
        assertEquals(ApplicationStatus.REFERRED, savedApplication().getStatus());
    }

    @Test
    @DisplayName("Rejected: 422 with the reason, and the application is kept (not rolled back)")
    void rejectedReturns422ButTheRecordIsKept() throws Exception {
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.reject("Active policy on the vehicle"));
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"))
                .andExpect(jsonPath("$.message").value("Application rejected"))
                .andExpect(jsonPath("$.details[0].reason").value("Active policy on the vehicle"));

        Application saved = savedApplication();
        assertNotNull(saved, "a rejected application must stay in the database");
        assertEquals(ApplicationStatus.REJECTED, saved.getStatus());
        assertEquals("Active policy on the vehicle", saved.getRejectionReason());
        List<ApplicationHistory> history = historyRepository.findByApplicationIdOrderByIdAsc(saved.getId());
        assertEquals(2, history.size());
        assertEquals(ApplicationStatus.REJECTED, history.get(1).getToStatus());
    }

    @Test
    @DisplayName("Oversized payload: 422 VALID-422")
    void oversizedPayloadReturns422() throws Exception {
        String hugeString = "x".repeat(4001);
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "quoteId", quote.getId(),
                "details", Map.of("largeField", hugeString),
                "documentIds", List.of(301L)));
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
        assertNull(savedApplication());
    }

    @Test
    @DisplayName("Vehicle reg no too long: 422 VALID-422")
    void vehicleRegNoTooLongReturns422() throws Exception {
        String requestJson = objectMapper.writeValueAsString(Map.of(
                "quoteId", quote.getId(),
                "details", Map.of("vehicleRegNo", "KA22AB1234567890123456"),
                "documentIds", List.of(301L)));
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(requestJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
        assertNull(savedApplication());
    }

    @Test
    @DisplayName("KYC not verified: 403 and nothing is saved")
    void unverifiedKycReturns403() throws Exception {
        when(kycService.isVerified(1001L)).thenReturn(false);
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
        assertNull(savedApplication());
    }

    @Test
    @DisplayName("Missing document: 422 and nothing is saved")
    void missingDocumentReturns422() throws Exception {
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALID-422"));
        assertNull(savedApplication());
    }

    @Test
    @DisplayName("Submitting the same quote twice: second call is 409")
    void secondSubmissionReturns409() throws Exception {
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE-409"));
    }

    @Test
    @DisplayName("Concurrent duplicate submissions for same quote results in exactly 1 success (201) and 1 conflict (409)")
    void concurrentDuplicateSubmissionsHandleRaceSafely() throws Exception {
        int threads = 2;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threads);

        List<Integer> statuses = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_CUSTOMER);
                readyLatch.countDown();
                try {
                    startLatch.await();
                    var mvcResult = mockMvc.perform(post("/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(List.of(301L))))
                            .andReturn();
                    statuses.add(mvcResult.getResponse().getStatus());
                } catch (Exception e) {
                    fail("Unexpected failure during concurrent submission: " + e.getMessage());
                } finally {
                    TestCurrentUserProvider.clearSyntheticUser();
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(2, statuses.size());
        assertTrue(statuses.contains(201), "One request must succeed with 201 Created");
        assertTrue(statuses.contains(409), "One request must fail with 409 Conflict");

        long count = applicationRepository.findAll().stream()
                .filter(a -> quote.getId().equals(a.getQuoteId()))
                .count();
        assertEquals(1, count, "Exactly 1 Application record must exist for the quote in the database");
    }

    @Test
    @DisplayName("Only customers may apply: an insurer admin gets 403")
    void staffRoleIsForbidden() throws Exception {
        TestCurrentUserProvider.setSyntheticUser(TestCurrentUserProvider.SEEDED_INSURER_ADMIN);
        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body(List.of(301L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS-403"));
        assertNull(savedApplication());
    }
}
