package com.team11.insurance.payments;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.PaymentFailedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.payments.dto.ApplicationPaymentSummary;
import com.team11.insurance.payments.dto.PaymentResponse;
import com.team11.insurance.payments.simulator.PaymentGatewaySimulator;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyStatus;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("P4-17 PaymentService Business Logic & Security Tests")
class PaymentServiceTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationHistoryRepository applicationHistoryRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PaymentServiceStub paymentServiceStub;

    @Autowired
    private PaymentGatewaySimulator paymentGatewaySimulator;

    @Autowired
    private PaymentService paymentService;

    private static final CurrentUser CUSTOMER_1001 = new CurrentUser(1001L, UserRole.CUSTOMER, null);
    private static final CurrentUser OTHER_CUSTOMER = new CurrentUser(9999L, UserRole.CUSTOMER, null);

    private Application testApp;
    private Quote testQuote;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    @DisplayName("Payment success: derives amount from quote/application, marks PAID, issues Policy")
    @Transactional
    void testProcessPayment_Success() {
        PaymentResponse response = paymentService.processPayment(testApp.getId(), CUSTOMER_1001);

        assertNotNull(response);
        assertEquals("Success", response.paymentStatus());
        assertNotNull(response.policyNo());
        assertTrue(response.policyNo().startsWith("POL-"));
        assertEquals(new BigDecimal("8450.00"), response.amount());

        Application updated = applicationRepository.findById(testApp.getId()).orElseThrow();
        assertEquals(ApplicationStatus.PAID, updated.getStatus());

        Optional<Policy> issuedPolicy = policyRepository.findByApplicationId(testApp.getId());
        assertTrue(issuedPolicy.isPresent());
        assertEquals(PolicyStatus.ACTIVE, issuedPolicy.get().getStatus());
        assertEquals(new BigDecimal("8450.00"), issuedPolicy.get().getPremiumPaid());
        assertEquals(1001L, issuedPolicy.get().getCustomerId());
        assertEquals("KA22AB1234", issuedPolicy.get().getVehicleRegNo());
    }

    @Test
    @DisplayName("Amount derivation: amount is derived server-side from stored Quote, not client")
    @Transactional
    void testProcessPayment_ServerDerivedAmount() {
        testQuote.setPremium(new BigDecimal("12500.00"));
        quoteRepository.save(testQuote);

        PaymentResponse response = paymentService.processPayment(testApp.getId(), CUSTOMER_1001);

        assertEquals(new BigDecimal("12500.00"), response.amount());
        Policy policy = policyRepository.findByApplicationId(testApp.getId()).orElseThrow();
        assertEquals(new BigDecimal("12500.00"), policy.getPremiumPaid());
    }

    @Test
    @DisplayName("Payment failure: throws PaymentFailedException (402), keeps status APPROVED, allows retry")
    @Transactional
    void testProcessPayment_Fails402_RetryAllowed() {
        paymentGatewaySimulator.setMode(PaymentGatewaySimulator.SimulatorMode.DECLINED);

        PaymentFailedException ex = assertThrows(PaymentFailedException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );
        assertTrue(ex.getMessage().contains("declined") || ex.getMessage().contains("failed"));

        Application appAfterFail = applicationRepository.findById(testApp.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPROVED, appAfterFail.getStatus(),
                "Application must remain APPROVED after failed payment to allow retry");

        Optional<Policy> policy = policyRepository.findByApplicationId(testApp.getId());
        assertTrue(policy.isEmpty(), "No policy must be issued for a failed payment");
    }

    @Test
    @DisplayName("Retry flow: after 402 failure, subsequent attempt with new key succeeds and transitions to PAID")
    @Transactional
    void testProcessPayment_RetrySucceedsAfterFailure() {
        paymentGatewaySimulator.setMode(PaymentGatewaySimulator.SimulatorMode.DECLINED);

        assertThrows(PaymentFailedException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );

        Application afterFirst = applicationRepository.findById(testApp.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPROVED, afterFirst.getStatus());

        // Now gateway recovers
        paymentGatewaySimulator.setMode(PaymentGatewaySimulator.SimulatorMode.SUCCESS);

        PaymentResponse response = paymentService.processPayment(testApp.getId(), CUSTOMER_1001);
        assertEquals("Success", response.paymentStatus());

        Application afterSecond = applicationRepository.findById(testApp.getId()).orElseThrow();
        assertEquals(ApplicationStatus.PAID, afterSecond.getStatus());

        Optional<Policy> policy = policyRepository.findByApplicationId(testApp.getId());
        assertTrue(policy.isPresent());
    }

    @Test
    @DisplayName("Conflict 409: attempting to pay an application not in APPROVED status throws StateConflictException")
    void testProcessPayment_ConflictWhenNotApproved() {
        testApp.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(testApp);

        StateConflictException ex = assertThrows(StateConflictException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );
        assertTrue(ex.getMessage().contains("not approved") || ex.getMessage().contains("cannot move"));

        testApp.setStatus(ApplicationStatus.REFERRED);
        applicationRepository.save(testApp);
        assertThrows(StateConflictException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );

        testApp.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(testApp);
        assertThrows(StateConflictException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );
    }

    @Test
    @DisplayName("Conflict 409: attempting to pay an already PAID application throws StateConflictException (duplicate charge protection)")
    void testProcessPayment_ConflictWhenAlreadyPaid() {
        testApp.setStatus(ApplicationStatus.PAID);
        applicationRepository.save(testApp);

        StateConflictException ex = assertThrows(StateConflictException.class, () ->
                paymentService.processPayment(testApp.getId(), CUSTOMER_1001)
        );
        assertTrue(ex.getMessage().contains("already") || ex.getMessage().contains("PAID"));
    }

    @Test
    @DisplayName("Access denied 403: paying another customer's application throws AccessDeniedException")
    void testProcessPayment_AccessDeniedForDifferentCustomer() {
        assertThrows(AccessDeniedException.class, () ->
                paymentService.processPayment(testApp.getId(), OTHER_CUSTOMER)
        );
    }

    @Test
    @DisplayName("Not found 404: paying non-existent application throws ResourceNotFoundException")
    void testProcessPayment_NotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processPayment(99999L, CUSTOMER_1001)
        );
    }

    @Test
    @DisplayName("Summary retrieval: returns application summary and verified amount")
    void testGetApplicationSummary() {
        ApplicationPaymentSummary summary = paymentService.getApplicationSummary(testApp.getId(), CUSTOMER_1001);

        assertNotNull(summary);
        assertEquals(testApp.getId(), summary.applicationId());
        assertEquals(1001L, summary.customerId());
        assertEquals("APPROVED", summary.status());
        assertEquals(new BigDecimal("8450.00"), summary.premium());
    }
}
