package com.team11.insurance.policies;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.claims.IClaimService;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.filestore.StoredDocument;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.payments.IPaymentService;
import com.team11.insurance.payments.PaymentResult;
import com.team11.insurance.payments.PaymentType;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PolicyLifecycleExtensionTest {

    private ApplicationRepository applicationRepository;
    private PolicyRepository policyRepository;
    private CancellationRequestRepository cancellationRequestRepository;
    private PolicyService policyService;
    private PolicyPdfGenerator policyPdfGenerator;
    private IFileStore fileStore;
    private QuoteRepository quoteRepository;
    private PlanRepository planRepository;
    private IClaimService claimService;
    private IPaymentService paymentService;
    private INotify notifyService;
    private IAuditLog auditLog;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        policyRepository = mock(PolicyRepository.class);
        cancellationRequestRepository = mock(CancellationRequestRepository.class);
        policyService = mock(PolicyService.class);
        policyPdfGenerator = mock(PolicyPdfGenerator.class);
        fileStore = mock(IFileStore.class);
        quoteRepository = mock(QuoteRepository.class);
        planRepository = mock(PlanRepository.class);
        claimService = mock(IClaimService.class);
        paymentService = mock(IPaymentService.class);
        notifyService = mock(INotify.class);
        auditLog = mock(IAuditLog.class);
        fixedClock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> providerOf(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    @Test
    void recoveryScheduler_issuesPolicyForPaidApplicationsWithoutPolicy() {
        Application paidApp = new Application();
        paidApp.setId(701L);
        paidApp.setCustomerId(101L);
        paidApp.setInsurerId(201L);
        paidApp.setPlanId(301L);
        paidApp.setQuoteId(401L);
        paidApp.setPremium(new BigDecimal("8500"));
        paidApp.setStatus(ApplicationStatus.PAID);

        when(applicationRepository.findByStatus(ApplicationStatus.PAID)).thenReturn(List.of(paidApp));
        when(policyRepository.existsByApplicationId(701L)).thenReturn(false);

        Policy issuedPolicy = new Policy();
        issuedPolicy.setPolicyNo("POL-2026-00701");
        when(policyService.issuePolicy(eq(paidApp), eq(new BigDecimal("8500")))).thenReturn(issuedPolicy);

        PolicyIssuanceRecoveryScheduler scheduler = new PolicyIssuanceRecoveryScheduler(
                applicationRepository,
                policyRepository,
                policyService,
                providerOf(quoteRepository),
                providerOf(auditLog)
        );

        int count = scheduler.runIssuanceRecoveryJob();
        assertEquals(1, count);
        verify(policyService).issuePolicy(paidApp, new BigDecimal("8500"));
    }

    @Test
    void renewalQuoteAndRenew_withinWindow_computesPremiumAndIssuesRenewedPolicy() {
        Policy existing = new Policy();
        existing.setPolicyNo("POL-2025-00100");
        existing.setCustomerId(101L);
        existing.setInsurerId(201L);
        existing.setPlanId(301L);
        existing.setVehicleRegNo("KA01AB1234");
        existing.setPremiumPaid(new BigDecimal("8000"));
        existing.setStartDate(LocalDate.of(2025, 6, 1));
        existing.setEndDate(LocalDate.of(2026, 5, 31));
        existing.setStatus(PolicyStatus.ACTIVE);

        Plan plan = new Plan(
                "MOTOR-4W-01", 201L, "MOTOR_FOUR_WHEELER", "SafeShield 4W",
                new BigDecimal("400000"), new BigDecimal("1000"), 0,
                new BigDecimal("5000"), "OD=2%; AGE=5-10:5%; DEP=10%", "{}", "{}", true
        );

        when(policyRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(planRepository.findById(301L)).thenReturn(Optional.of(plan));
        when(claimService.hasPaidClaim(any())).thenReturn(false);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(policyPdfGenerator.generatePolicyPdf(any(), anyString(), anyString())).thenReturn(new byte[]{1, 2, 3});
        when(fileStore.storeStream(any(ByteArrayInputStream.class), anyString(), anyString(), anyLong(), eq(DocumentCategory.POLICY), anyLong()))
                .thenReturn(new StoredDocument(99L, "key", "policy.pdf", "application/pdf", 3L, DocumentCategory.POLICY, "sha", 101L, Instant.now()));
        when(paymentService.charge(anyLong(), any(), anyString()))
                .thenReturn(PaymentResult.success(1L, "key", "TXN-1", new BigDecimal("6804"), PaymentType.INITIAL_PURCHASE, fixedClock));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyRenewalController controller = new PolicyRenewalController(
                policyRepository,
                policyPdfGenerator,
                fileStore,
                providerOf(quoteRepository),
                providerOf(planRepository),
                providerOf(null),
                providerOf(claimService),
                providerOf(paymentService),
                providerOf(notifyService),
                providerOf(auditLog),
                providerOf(fixedClock)
        );

        CurrentUser customer = new CurrentUser(101L, UserRole.CUSTOMER, null);

        ResponseEntity<PolicyRenewalController.RenewalQuoteResponse> quoteRes =
                controller.createRenewalQuote(10L, customer);
        assertEquals(HttpStatus.OK, quoteRes.getStatusCode());
        assertNotNull(quoteRes.getBody());
        assertEquals(new BigDecimal("6804"), quoteRes.getBody().premium());

        ResponseEntity<PolicyRenewalController.RenewalResponse> renewRes =
                controller.renewPolicy(10L, new PolicyRenewalController.RenewalConfirmRequest(null), customer);
        assertEquals(HttpStatus.OK, renewRes.getStatusCode());
        assertNotNull(renewRes.getBody());
        assertEquals(PolicyStatus.ACTIVE, renewRes.getBody().status());
        verify(notifyService).send(eq(NotificationEvent.POLICY_RENEWED), eq(101L), anyMap());
    }

    @Test
    void renewalQuote_outsideWindow_throws409StateConflict() {
        Policy farFuturePolicy = new Policy();
        farFuturePolicy.setPolicyNo("POL-2026-00999");
        farFuturePolicy.setCustomerId(101L);
        farFuturePolicy.setInsurerId(201L);
        farFuturePolicy.setPlanId(301L);
        farFuturePolicy.setPremiumPaid(new BigDecimal("8000"));
        farFuturePolicy.setStartDate(LocalDate.of(2026, 1, 1));
        farFuturePolicy.setEndDate(LocalDate.of(2026, 12, 31));
        farFuturePolicy.setStatus(PolicyStatus.ACTIVE);

        when(policyRepository.findById(55L)).thenReturn(Optional.of(farFuturePolicy));

        PolicyRenewalController controller = new PolicyRenewalController(
                policyRepository, policyPdfGenerator, fileStore,
                providerOf(quoteRepository), providerOf(planRepository), providerOf(null),
                providerOf(claimService), providerOf(paymentService),
                providerOf(notifyService), providerOf(auditLog), providerOf(fixedClock)
        );

        CurrentUser customer = new CurrentUser(101L, UserRole.CUSTOMER, null);
        assertThrows(StateConflictException.class, () -> controller.createRenewalQuote(55L, customer));
    }

    @Test
    void cancellationRequestAndDecision_day16Oracle10873_andSettledClaimZero() {
        Clock day16Clock = Clock.fixed(Instant.parse("2026-01-16T10:00:00Z"), ZoneOffset.UTC);

        Policy activePolicy = new Policy();
        activePolicy.setPolicyNo("POL-2026-00500");
        activePolicy.setCustomerId(101L);
        activePolicy.setInsurerId(201L);
        activePolicy.setPlanId(301L);
        activePolicy.setPremiumPaid(new BigDecimal("12000"));
        activePolicy.setStartDate(LocalDate.of(2026, 1, 1));
        activePolicy.setEndDate(LocalDate.of(2026, 12, 31));
        activePolicy.setStatus(PolicyStatus.ACTIVE);

        when(policyRepository.findById(500L)).thenReturn(Optional.of(activePolicy));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        CancellationRequest savedReq = new CancellationRequest();
        savedReq.setPolicyId(500L);
        savedReq.setInsurerId(201L);
        savedReq.setRequestedBy(101L);
        savedReq.setReason("Sold car");
        savedReq.setStatus(CancellationStatus.REQUESTED);

        when(cancellationRequestRepository.save(any(CancellationRequest.class))).thenReturn(savedReq);
        when(cancellationRequestRepository.findById(901L)).thenReturn(Optional.of(savedReq));

        PolicyCancellationController controller = new PolicyCancellationController(
                policyRepository,
                cancellationRequestRepository,
                providerOf(claimService),
                providerOf(paymentService),
                providerOf(notifyService),
                providerOf(auditLog),
                providerOf(day16Clock)
        );

        CurrentUser customer = new CurrentUser(101L, UserRole.CUSTOMER, null);
        ResponseEntity<PolicyCancellationController.CancellationSubmitResponse> submitRes =
                controller.requestCancellation(500L, new PolicyCancellationController.CancellationSubmitRequest("Sold car"), customer);
        assertEquals(HttpStatus.CREATED, submitRes.getStatusCode());
        assertEquals(PolicyStatus.CANCELLATION_REQUESTED, activePolicy.getStatus());

        // Approve on Day 16 with no settled claim -> Rs 10,873
        when(claimService.hasPaidClaim(any())).thenReturn(false);
        CurrentUser insurerAdmin = new CurrentUser(201L, UserRole.INSURER_ADMIN, 201L);

        ResponseEntity<PolicyCancellationController.CancellationDecisionResponse> decisionRes =
                controller.decideCancellation(901L, new PolicyCancellationController.CancellationDecisionRequest("Approve", "Approved pro-rata"), insurerAdmin);

        assertEquals(HttpStatus.OK, decisionRes.getStatusCode());
        assertNotNull(decisionRes.getBody());
        assertEquals("Approved", decisionRes.getBody().status());
        assertEquals(new BigDecimal("10873"), decisionRes.getBody().refundAmount());
        assertEquals(PolicyStatus.CANCELLED, activePolicy.getStatus());
    }
}
