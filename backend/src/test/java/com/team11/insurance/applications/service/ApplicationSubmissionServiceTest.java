package com.team11.insurance.applications.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.applications.dto.SubmitApplicationRequest;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.api.ErrorCode;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.AppException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.kyc.IKycStatus;
import com.team11.insurance.plans.IPlanQuery;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;
import com.team11.insurance.underwriting.IUnderwriting;
import com.team11.insurance.underwriting.UnderwritingResult;

/** TC-App-01 and the rules in contract docs/contracts/p3-contract.md section 3.1 (no Spring, no database). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationSubmissionServiceTest {

    private static final long USER_ID = 1001L;
    private static final long QUOTE_ID = 5021L;
    private static final long APPLICATION_ID = 7001L;

    @Mock private IKycStatus kyc;
    @Mock private QuoteRepository quotes;
    @Mock private IPlanQuery plans;
    @Mock private ApplicationRepository applications;
    @Mock private ApplicationHistoryRepository histories;
    @Mock private IUnderwriting underwriting;
    @Mock private IAuditLog audit;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC);
    private final CurrentUser customer = new CurrentUser(USER_ID, UserRole.CUSTOMER, null);
    private final AtomicReference<Application> stored = new AtomicReference<>();
    private ApplicationSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationSubmissionService(kyc, quotes, plans, applications, histories, underwriting,
                audit, new ObjectMapper(), TransactionOperations.withoutTransaction(), clock);
        when(kyc.isVerified(USER_ID)).thenReturn(true);
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(quote(USER_ID, 10L, Instant.parse("2026-10-21T10:00:00Z"))));
        when(applications.saveAndFlush(any())).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", APPLICATION_ID);
            stored.set(a);
            return a;
        });
        when(applications.findById(APPLICATION_ID)).thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(applications.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.approve("auto"));
    }

    private static Quote quote(Long customerId, Long insurerId, Instant validUntil) {
        Quote q = new Quote(customerId, insurerId, 1L, "MOTOR_FOUR_WHEELER", new BigDecimal("8640"), "{}", "{}", validUntil);
        ReflectionTestUtils.setField(q, "id", QUOTE_ID);
        return q;
    }

    private static SubmitApplicationRequest request(List<Long> documentIds) {
        return new SubmitApplicationRequest(QUOTE_ID, Map.of("vehicleRegNo", " ka22ab1234 "), documentIds);
    }

    private static void assertCode(ErrorCode expected, AppException e) {
        assertEquals(expected, e.getErrorCode());
    }

    // ---- guards: nothing is saved ----

    @Test
    void unverifiedKycIs403AndNothingIsSaved() {
        when(kyc.isVerified(USER_ID)).thenReturn(false);
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> service.submit(customer, request(List.of(301L))));
        assertCode(ErrorCode.ACCESS_403, e);
        verifyNoInteractions(applications, histories, underwriting, audit);
    }

    @Test
    void missingDocumentsIs422AndNothingIsSaved() {
        for (List<Long> docs : java.util.Arrays.asList(null, List.<Long>of())) {
            ValidationException e = assertThrows(ValidationException.class, () -> service.submit(customer, request(docs)));
            assertCode(ErrorCode.VALID_422, e);
        }
        assertThrows(ValidationException.class,
                () -> service.submit(customer, request(java.util.Arrays.asList(301L, null))));
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void anotherCustomersQuoteLooksLikeNotFound() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(quote(9999L, 10L, null)));
        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> service.submit(customer, request(List.of(301L))));
        assertCode(ErrorCode.NOTFOUND_404, e);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void expiredQuoteIs422() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(quote(USER_ID, 10L, clock.instant().minus(1, ChronoUnit.SECONDS))));
        assertThrows(ValidationException.class, () -> service.submit(customer, request(List.of(301L))));
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void secondSubmissionOfTheSameQuoteIs409() {
        when(applications.existsByQuoteId(QUOTE_ID)).thenReturn(true);
        StateConflictException e = assertThrows(StateConflictException.class,
                () -> service.submit(customer, request(List.of(301L))));
        assertCode(ErrorCode.STATE_409, e);
        verify(applications, never()).saveAndFlush(any());
    }

    @Test
    void insurerFallsBackToThePlanWhenTheQuoteHasNone() {
        when(quotes.findById(QUOTE_ID)).thenReturn(Optional.of(quote(USER_ID, null, null)));
        Plan plan = mock(Plan.class);
        when(plan.getInsurerId()).thenReturn(77L);
        when(plans.getPlan(1L)).thenReturn(plan);
        service.submit(customer, request(List.of(301L)));
        assertEquals(77L, stored.get().getInsurerId());
    }

    // ---- outcomes ----

    @Test
    void approvedApplicationIsSavedAsSubmittedFirstThenApproved() {
        SubmissionResult result = service.submit(customer, request(List.of(301L, 302L, 301L)));

        assertEquals(ApplicationStatus.APPROVED, result.status());
        assertEquals(APPLICATION_ID, result.applicationId());

        InOrder order = inOrder(applications, underwriting);
        order.verify(applications).saveAndFlush(any());
        order.verify(underwriting).evaluate(any());

        Application a = stored.get();
        assertEquals(ApplicationStatus.APPROVED, a.getStatus());
        assertEquals(USER_ID, a.getCustomerId());
        assertEquals(10L, a.getInsurerId());
        assertEquals(QUOTE_ID, a.getQuoteId());
        assertEquals("KA22AB1234", a.getVehicleRegNo());
        assertEquals("301,302", a.getDocumentIds());                      // duplicates removed
        assertEquals(0, new BigDecimal("8640").compareTo(a.getPremium()));

        ArgumentCaptor<ApplicationHistory> history = ArgumentCaptor.forClass(ApplicationHistory.class);
        verify(histories, times(2)).save(history.capture());
        assertNull(history.getAllValues().get(0).getFromStatus());
        assertEquals(ApplicationStatus.SUBMITTED, history.getAllValues().get(0).getToStatus());
        assertEquals(ApplicationStatus.SUBMITTED, history.getAllValues().get(1).getFromStatus());
        assertEquals(ApplicationStatus.APPROVED, history.getAllValues().get(1).getToStatus());

        verify(audit).record(eq(USER_ID), eq("APPLICATION_SUBMITTED"), eq("Application"), eq(APPLICATION_ID), any());
    }

    @Test
    void referredApplicationStaysOpenForAnUnderwriter() {
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.refer("high IDV"));
        SubmissionResult result = service.submit(customer, request(List.of(301L)));
        assertEquals(ApplicationStatus.REFERRED, result.status());
        assertEquals(ApplicationStatus.REFERRED, stored.get().getStatus());
        assertNull(stored.get().getRejectionReason());
    }

    @Test
    void rejectedApplicationIsKeptWithItsReason() {
        when(underwriting.evaluate(any())).thenReturn(UnderwritingResult.reject("Active policy on the vehicle"));
        SubmissionResult result = service.submit(customer, request(List.of(301L)));
        assertEquals(ApplicationStatus.REJECTED, result.status());
        assertEquals("Active policy on the vehicle", result.reason());
        assertEquals(ApplicationStatus.REJECTED, stored.get().getStatus());
        assertEquals("Active policy on the vehicle", stored.get().getRejectionReason());
        verify(histories, times(2)).save(any());
    }

    @Test
    void ifUnderwritingFailsTheApplicationStaysSubmitted() {
        when(underwriting.evaluate(any())).thenThrow(new IllegalStateException("engine down"));
        assertThrows(IllegalStateException.class, () -> service.submit(customer, request(List.of(301L))));
        assertEquals(ApplicationStatus.SUBMITTED, stored.get().getStatus());
        verify(histories, times(1)).save(any());
        verifyNoInteractions(audit);
    }
}
