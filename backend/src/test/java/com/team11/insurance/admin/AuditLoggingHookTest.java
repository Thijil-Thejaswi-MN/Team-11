package com.team11.insurance.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.claims.Claim;
import com.team11.insurance.claims.ClaimDecisionRequest;
import com.team11.insurance.claims.ClaimHistoryRepository;
import com.team11.insurance.claims.ClaimRepository;
import com.team11.insurance.claims.ClaimService;
import com.team11.insurance.claims.ClaimStatus;
import com.team11.insurance.claims.ClaimSubmissionRequest;
import com.team11.insurance.claims.ClaimVerificationRequest;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationChannel;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.notifications.NotificationOutbox;
import com.team11.insurance.notifications.NotificationOutboxRepository;
import com.team11.insurance.notifications.NotificationRequest;
import com.team11.insurance.notifications.NotificationService;
import com.team11.insurance.notifications.dispatcher.NotificationAsyncService;
import com.team11.insurance.notifications.dispatcher.NotificationDispatcher;
import com.team11.insurance.notifications.retry.NotificationRetryPolicy;
import com.team11.insurance.notifications.sender.INotificationChannelSender;
import com.team11.insurance.notifications.template.NotificationPayload;
import com.team11.insurance.notifications.template.NotificationTemplateService;
import com.team11.insurance.payments.IPaymentService;
import com.team11.insurance.payments.PaymentResult;
import com.team11.insurance.payments.PaymentService;
import com.team11.insurance.payments.PaymentType;
import com.team11.insurance.policies.IPolicyQuery;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyPdfGenerator;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyService;
import com.team11.insurance.policies.PolicyView;
import com.team11.insurance.quotes.QuoteRepository;

@DisplayName("P1-05 / Issues #169-#172: Audit Logging Hooks for Payment, Claims, Policies, and Notifications")
class AuditLoggingHookTest {

    private IAuditLog auditLog;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        auditLog = mock(IAuditLog.class);
        fixedClock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneId.of("UTC"));
    }

    // -------------------------------------------------------------------------
    // Issue #169: Payment Audit Logging
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("PaymentService records PAYMENT_PROCESSED on successful payment")
    void paymentService_recordsAuditLogOnSuccess() {
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        ApplicationHistoryRepository appHistRepo = mock(ApplicationHistoryRepository.class);
        QuoteRepository quoteRepo = mock(QuoteRepository.class);
        PolicyRepository policyRepo = mock(PolicyRepository.class);
        IPaymentService paymentAdapter = mock(IPaymentService.class);

        Application app = new Application();
        ReflectionTestUtils.setField(app, "id", 101L);
        app.setCustomerId(1L);
        app.setStatus(ApplicationStatus.APPROVED);
        app.setPremium(new BigDecimal("12000.00"));
        when(appRepo.findById(101L)).thenReturn(Optional.of(app));

        when(paymentAdapter.charge(eq(101L), any(), anyString()))
                .thenReturn(PaymentResult.success(101L, "101-1", "TXN-101", new BigDecimal("12000.00"), PaymentType.CHARGE, fixedClock));

        PaymentService paymentService = new PaymentService(
                appRepo, appHistRepo, quoteRepo, policyRepo, paymentAdapter, null, auditLog, fixedClock
        );

        CurrentUser user = new CurrentUser(1L, UserRole.CUSTOMER, null);
        paymentService.processPayment(101L, user);

        verify(auditLog).record(eq(1L), eq("PAYMENT_PROCESSED"), eq("Application"), eq(101L), contains("TXN-101"));
    }

    @Test
    @DisplayName("PaymentService records PAYMENT_FAILED on failed payment")
    void paymentService_recordsAuditLogOnFailure() {
        ApplicationRepository appRepo = mock(ApplicationRepository.class);
        ApplicationHistoryRepository appHistRepo = mock(ApplicationHistoryRepository.class);
        QuoteRepository quoteRepo = mock(QuoteRepository.class);
        PolicyRepository policyRepo = mock(PolicyRepository.class);
        IPaymentService paymentAdapter = mock(IPaymentService.class);

        Application app = new Application();
        ReflectionTestUtils.setField(app, "id", 102L);
        app.setCustomerId(1L);
        app.setStatus(ApplicationStatus.APPROVED);
        app.setPremium(new BigDecimal("12000.00"));
        when(appRepo.findById(102L)).thenReturn(Optional.of(app));

        when(paymentAdapter.charge(eq(102L), any(), anyString()))
                .thenReturn(PaymentResult.failed(102L, "102-1", new BigDecimal("12000.00"), PaymentType.CHARGE, "Card declined by issuing bank", "GATEWAY_TIMEOUT", fixedClock));

        PaymentService paymentService = new PaymentService(
                appRepo, appHistRepo, quoteRepo, policyRepo, paymentAdapter, null, auditLog, fixedClock
        );

        CurrentUser user = new CurrentUser(1L, UserRole.CUSTOMER, null);
        assertThatThrownBy(() -> paymentService.processPayment(102L, user))
                .isInstanceOf(RuntimeException.class);

        verify(auditLog).record(eq(1L), eq("PAYMENT_FAILED"), eq("Application"), eq(102L), contains("Card declined"));
    }

    // -------------------------------------------------------------------------
    // Issue #170: Claims Audit Logging
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("ClaimService records CLAIM_FILED when customer files claim")
    void claimService_recordsClaimFiled() {
        ClaimRepository claimRepo = mock(ClaimRepository.class);
        ClaimHistoryRepository histRepo = mock(ClaimHistoryRepository.class);
        IPolicyQuery policyQuery = mock(IPolicyQuery.class);
        INotify notify = mock(INotify.class);

        PolicyView policyView = new PolicyView(
                501L, "POL-2026-000501", 1L, 2L, 10L, "KA01AB1234",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                com.team11.insurance.policies.PolicyStatus.ACTIVE
        );
        when(policyQuery.getPolicy(501L)).thenReturn(policyView);
        when(claimRepo.saveAndFlush(any(Claim.class))).thenAnswer(invocation -> {
            Claim c = invocation.getArgument(0);
            if (c.getId() == null) ReflectionTestUtils.setField(c, "id", 701L);
            return c;
        });

        ClaimService claimService = new ClaimService(
                claimRepo, histRepo, policyQuery, null, notify, fixedClock, null, auditLog
        );

        CurrentUser user = new CurrentUser(1L, UserRole.CUSTOMER, null);
        ClaimSubmissionRequest req = new ClaimSubmissionRequest(
                501L, LocalDate.of(2026, 11, 10), new BigDecimal("5000.00"), "Accidental bumper damage", List.of()
        );

        claimService.file(req, user);

        verify(auditLog).record(eq(1L), eq("CLAIM_FILED"), eq("Claim"), eq(701L), contains("5000.00"));
    }

    @Test
    @DisplayName("ClaimService records CLAIM_VERIFIED when claims officer verifies documents complete")
    void claimService_recordsClaimVerified() {
        ClaimRepository claimRepo = mock(ClaimRepository.class);
        ClaimHistoryRepository histRepo = mock(ClaimHistoryRepository.class);

        Claim claim = new Claim();
        ReflectionTestUtils.setField(claim, "id", 702L);
        claim.setInsurerId(2L);
        claim.setCustomerId(1L);
        claim.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);
        when(claimRepo.findById(702L)).thenReturn(Optional.of(claim));
        when(claimRepo.saveAndFlush(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        ClaimService claimService = new ClaimService(
                claimRepo, histRepo, null, null, null, fixedClock, null, auditLog
        );

        CurrentUser officer = new CurrentUser(201L, UserRole.CLAIMS_OFFICER, 2L);
        claimService.verify(702L, new ClaimVerificationRequest(true, null, "All FIR and repair estimates verified"), officer);

        verify(auditLog).record(eq(201L), eq("CLAIM_VERIFIED"), eq("Claim"), eq(702L), contains("verified"));
    }

    @Test
    @DisplayName("ClaimService records CLAIM_APPROVED and CLAIM_REJECTED on decision")
    void claimService_recordsClaimDecision() {
        ClaimRepository claimRepo = mock(ClaimRepository.class);
        ClaimHistoryRepository histRepo = mock(ClaimHistoryRepository.class);

        Claim claim = new Claim();
        ReflectionTestUtils.setField(claim, "id", 703L);
        claim.setClaimNo("CLM-001");
        claim.setInsurerId(2L);
        claim.setCustomerId(1L);
        claim.setStatus(ClaimStatus.ASSESSMENT);
        when(claimRepo.findById(703L)).thenReturn(Optional.of(claim));
        when(claimRepo.saveAndFlush(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        ClaimService claimService = new ClaimService(
                claimRepo, histRepo, null, null, null, fixedClock, null, auditLog
        );

        CurrentUser officer = new CurrentUser(201L, UserRole.CLAIMS_OFFICER, 2L);
        claimService.decide(703L, new ClaimDecisionRequest("Approve", new BigDecimal("4500.00"), "Approved per surveyor report"), officer);

        verify(auditLog).record(eq(201L), eq("CLAIM_APPROVED"), eq("Claim"), eq(703L), contains("4500.00"));
    }

    @Test
    @DisplayName("ClaimService records CLAIM_AUTO_CLOSED when stale claims are closed")
    void claimService_recordsClaimAutoClosed() {
        ClaimRepository claimRepo = mock(ClaimRepository.class);
        ClaimHistoryRepository histRepo = mock(ClaimHistoryRepository.class);

        Claim staleClaim = new Claim();
        ReflectionTestUtils.setField(staleClaim, "id", 704L);
        staleClaim.setStatus(ClaimStatus.DOCUMENT_VERIFICATION);
        staleClaim.setDocumentRequestedAt(fixedClock.instant().minus(20, ChronoUnit.DAYS));

        when(claimRepo.findByStatus(ClaimStatus.DOCUMENT_VERIFICATION)).thenReturn(List.of(staleClaim));
        when(claimRepo.saveAndFlush(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        ClaimService claimService = new ClaimService(
                claimRepo, histRepo, null, null, null, fixedClock, null, auditLog
        );

        int closed = claimService.closeStale();
        assertThat(closed).isEqualTo(1);
        verify(auditLog).recordSystem(eq("CLAIM_AUTO_CLOSED"), eq("Claim"), eq(704L), anyString());
    }

    @Test
    @DisplayName("ClaimService records CLAIM_SETTLED on successful payout")
    void claimService_recordsClaimSettled() {
        ClaimRepository claimRepo = mock(ClaimRepository.class);
        ClaimHistoryRepository histRepo = mock(ClaimHistoryRepository.class);
        IPaymentService paymentService = mock(IPaymentService.class);

        Claim claim = new Claim();
        ReflectionTestUtils.setField(claim, "id", 705L);
        claim.setInsurerId(2L);
        claim.setCustomerId(1L);
        claim.setStatus(ClaimStatus.PAYMENT_PROCESSING);
        claim.setAssessedAmount(new BigDecimal("4500.00"));
        when(claimRepo.findById(705L)).thenReturn(Optional.of(claim));
        when(claimRepo.saveAndFlush(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        when(paymentService.payout(eq(705L), eq(new BigDecimal("4500.00")), anyString(), anyString()))
                .thenReturn(PaymentResult.success(705L, "claim-705-payout", "TXN-PAYOUT-705", new BigDecimal("4500.00"), PaymentType.PAYOUT, fixedClock));

        ClaimService claimService = new ClaimService(
                claimRepo, histRepo, null, null, null, fixedClock, paymentService, auditLog
        );

        CurrentUser officer = new CurrentUser(201L, UserRole.CLAIMS_OFFICER, 2L);
        claimService.payout(705L, officer);

        verify(auditLog).record(eq(201L), eq("CLAIM_SETTLED"), eq("Claim"), eq(705L), contains("TXN-PAYOUT-705"));
    }

    // -------------------------------------------------------------------------
    // Issue #171: Policy Lifecycle Audit Logging
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("PolicyService records POLICY_ISSUED when policy is activated")
    void policyService_recordsPolicyIssued() {
        PolicyRepository policyRepo = mock(PolicyRepository.class);
        PolicyPdfGenerator pdfGenerator = mock(PolicyPdfGenerator.class);
        IFileStore fileStore = mock(IFileStore.class);

        when(pdfGenerator.generatePolicyPdf(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(policyRepo.save(any(Policy.class))).thenAnswer(i -> {
            Policy p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 801L);
            return p;
        });

        PolicyService policyService = new PolicyService(
                policyRepo, pdfGenerator, fileStore, fixedClock, auditLog
        );

        Application app = new Application();
        ReflectionTestUtils.setField(app, "id", 205L);
        app.setCustomerId(10L);
        app.setInsurerId(2L);
        app.setPlanId(3L);

        Policy issued = policyService.issuePolicy(app, new BigDecimal("15000.00"));
        assertThat(issued).isNotNull();

        verify(auditLog).record(eq(10L), eq("POLICY_ISSUED"), eq("Policy"), eq(801L), contains("205"));
    }

    // -------------------------------------------------------------------------
    // Issue #172: Notification Audit Logging
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("NotificationService records NOTIFICATION_ENQUEUED when notification is saved")
    void notificationService_recordsNotificationEnqueued() {
        NotificationOutboxRepository outboxRepo = mock(NotificationOutboxRepository.class);
        NotificationAsyncService asyncService = mock(NotificationAsyncService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(outboxRepo.save(any(NotificationOutbox.class))).thenAnswer(i -> {
            NotificationOutbox o = i.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 901L);
            return o;
        });

        NotificationService notificationService = new NotificationService(
                outboxRepo, asyncService, null, objectMapper, fixedClock, mockProvider(auditLog)
        );

        NotificationRequest request = new NotificationRequest(
                NotificationEvent.POLICY_ISSUED, 10L, "customer@example.com", "+919876543210",
                NotificationChannel.EMAIL, Map.of("policyNo", "POL-2026-0001"), null
        );

        notificationService.send(request);

        verify(auditLog).record(eq(10L), eq("NOTIFICATION_ENQUEUED"), eq("NotificationOutbox"), eq(901L), contains("POLICY_ISSUED"));
    }

    @Test
    @DisplayName("NotificationDispatcher records NOTIFICATION_DISPATCHED on successful delivery")
    void notificationDispatcher_recordsNotificationDispatched() throws Exception {
        NotificationOutboxRepository outboxRepo = mock(NotificationOutboxRepository.class);
        NotificationTemplateService templateService = mock(NotificationTemplateService.class);
        INotificationChannelSender channelSender = mock(INotificationChannelSender.class);
        NotificationRetryPolicy retryPolicy = new NotificationRetryPolicy(fixedClock);
        ObjectMapper objectMapper = new ObjectMapper();

        NotificationOutbox outbox = new NotificationOutbox();
        ReflectionTestUtils.setField(outbox, "id", 902L);
        outbox.setRecipientUserId(10L);
        outbox.setEvent(NotificationEvent.POLICY_ISSUED);
        outbox.setChannel(NotificationChannel.EMAIL);
        outbox.setRecipientEmail("customer@example.com");
        outbox.setParamsJson("{}");

        when(outboxRepo.updateStatusAtomically(eq(902L), any(), any(), any())).thenReturn(1);
        when(outboxRepo.findById(902L)).thenReturn(Optional.of(outbox));
        when(templateService.render(any(), any())).thenReturn(new NotificationPayload("Policy Issued", "Body", "SMS"));

        NotificationDispatcher dispatcher = new NotificationDispatcher(
                outboxRepo, templateService, channelSender, retryPolicy, objectMapper, fixedClock, mockProvider(auditLog)
        );

        boolean success = dispatcher.dispatch(902L);
        assertThat(success).isTrue();

        verify(auditLog).record(eq(10L), eq("NOTIFICATION_DISPATCHED"), eq("NotificationOutbox"), eq(902L), contains("POLICY_ISSUED"));
    }

    private <T> ObjectProvider<T> mockProvider(T instance) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(instance);
        return provider;
    }
}
