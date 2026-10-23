package com.team11.insurance.payments;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationHistory;
import com.team11.insurance.applications.ApplicationHistoryRepository;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStateMachine;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.PaymentFailedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.common.exception.ValidationException;
import com.team11.insurance.payments.dto.ApplicationPaymentSummary;
import com.team11.insurance.payments.dto.PaymentResponse;
import com.team11.insurance.policies.Policy;
import com.team11.insurance.policies.PolicyRepository;
import com.team11.insurance.policies.PolicyService;
import com.team11.insurance.policies.PolicyStatus;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

/**
 * Service managing insurance application payment flows (P4-17 / INS-F-009).
 * <p>
 * Enforces:
 * <ul>
 *   <li>Server-derived payment amounts (never trust client-supplied amounts)</li>
 *   <li>Customer ownership and application approval verification</li>
 *   <li>Server-generated attempt idempotency keys ("applicationId-attemptNo")</li>
 *   <li>Clean state transitions (APPROVED -> PAID only upon adapter success)</li>
 *   <li>Protection against duplicate submissions and parallel double-clicks</li>
 *   <li>Retry support after failure without duplicate charges</li>
 * </ul>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository applicationHistoryRepository;
    private final QuoteRepository quoteRepository;
    private final PolicyRepository policyRepository;
    private final IPaymentService paymentAdapter;
    private final PolicyService policyService;
    private final Clock clock;

    private final ConcurrentHashMap<Long, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();
    private final Set<Long> activeApplicationPayments = ConcurrentHashMap.newKeySet();

    @Autowired
    public PaymentService(
            ApplicationRepository applicationRepository,
            ApplicationHistoryRepository applicationHistoryRepository,
            QuoteRepository quoteRepository,
            PolicyRepository policyRepository,
            IPaymentService paymentAdapter,
            ObjectProvider<PolicyService> policyServiceProvider,
            ObjectProvider<Clock> clockProvider) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "applicationRepository must not be null");
        this.applicationHistoryRepository = Objects.requireNonNull(applicationHistoryRepository, "applicationHistoryRepository must not be null");
        this.quoteRepository = Objects.requireNonNull(quoteRepository, "quoteRepository must not be null");
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
        this.paymentAdapter = Objects.requireNonNull(paymentAdapter, "paymentAdapter must not be null");
        this.policyService = policyServiceProvider.getIfAvailable();
        this.clock = clockProvider.getIfAvailable(Clock::systemDefaultZone);
    }

    public PaymentService(
            ApplicationRepository applicationRepository,
            ApplicationHistoryRepository applicationHistoryRepository,
            QuoteRepository quoteRepository,
            PolicyRepository policyRepository,
            IPaymentService paymentAdapter,
            Clock clock) {
        this(applicationRepository, applicationHistoryRepository, quoteRepository, policyRepository, paymentAdapter, null, clock);
    }

    public PaymentService(
            ApplicationRepository applicationRepository,
            ApplicationHistoryRepository applicationHistoryRepository,
            QuoteRepository quoteRepository,
            PolicyRepository policyRepository,
            IPaymentService paymentAdapter,
            PolicyService policyService,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.applicationHistoryRepository = applicationHistoryRepository;
        this.quoteRepository = quoteRepository;
        this.policyRepository = policyRepository;
        this.paymentAdapter = paymentAdapter;
        this.policyService = policyService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    /**
     * Retrieves application details and verified payable amount for the payment screen.
     */
    @Transactional(readOnly = true)
    public ApplicationPaymentSummary getApplicationSummary(Long applicationId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (applicationId == null || applicationId <= 0) {
            throw new ValidationException("Invalid application ID: " + applicationId);
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        RoleSecurity.assertCustomerOrStaffAccess(currentUser, app.getCustomerId());

        BigDecimal derivedPremium = derivePayableAmount(app);
        String planName = "Insurance Plan #" + app.getPlanId();

        return new ApplicationPaymentSummary(
                app.getId(),
                app.getCustomerId(),
                app.getPlanId(),
                planName,
                app.getProductType(),
                app.getVehicleRegNo(),
                derivedPremium,
                app.getStatus() != null ? app.getStatus().name() : "UNKNOWN"
        );
    }

    /**
     * Executes payment for an approved application.
     */
    @Transactional
    public PaymentResponse processPayment(Long applicationId, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Access denied: user context is missing.");
        }
        if (applicationId == null || applicationId <= 0) {
            throw new ValidationException("Valid applicationId is required for payment.");
        }

        // Prevent concurrent double-click submissions on the server
        if (!activeApplicationPayments.add(applicationId)) {
            throw new StateConflictException("Payment processing is already in progress for application " + applicationId);
        }

        try {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

            // Validate caller ownership
            RoleSecurity.assertCustomerOrStaffAccess(currentUser, application.getCustomerId());

            // Validate eligibility
            if (application.getStatus() == ApplicationStatus.PAID) {
                Optional<Policy> existingPolicy = policyRepository.findByApplicationId(application.getId());
                String policyInfo = existingPolicy.map(p -> " (Policy: " + p.getPolicyNo() + ")").orElse("");
                throw new StateConflictException("Application " + applicationId + " has already been paid" + policyInfo + ".");
            }

            if (application.getStatus() != ApplicationStatus.APPROVED) {
                throw new StateConflictException("Application " + applicationId + " is not approved for payment (current status: "
                        + application.getStatus() + "). Only APPROVED applications may be paid.");
            }

            // Derive payable amount strictly server-side from application and quote
            BigDecimal payableAmount = derivePayableAmount(application);

            // Mint server-side attempt-based idempotency key
            int attempt = attemptCounters.computeIfAbsent(applicationId, k -> new AtomicInteger(0)).incrementAndGet();
            String idempotencyKey = applicationId + "-" + attempt;

            log.info("Initiating payment for application {}, amount {}, idempotencyKey {}",
                    applicationId, payableAmount, idempotencyKey);

            // Call payment adapter
            PaymentResult result = paymentAdapter.charge(applicationId, payableAmount, idempotencyKey);

            if (result.status() == PaymentStatus.FAILED) {
                String failureMessage = result.failureReason() != null
                        ? result.failureReason()
                        : "Payment processing failed; retry allowed";
                log.warn("Payment failed for application {}: reason={}, code={}",
                        applicationId, failureMessage, result.errorCode());
                // Crucial: Application status remains APPROVED, allowing future retries
                throw new PaymentFailedException(failureMessage);
            }

            // Transition application state to PAID
            ApplicationStateMachine.requireTransition(application.getStatus(), ApplicationStatus.PAID);
            application.setStatus(ApplicationStatus.PAID);
            applicationRepository.save(application);

            // Record state transition in audit history
            Long actorId = currentUser != null ? currentUser.getUserId() : null;
            String auditReason = "Payment successful (Txn: " + result.transactionReference() + ")";
            applicationHistoryRepository.save(new ApplicationHistory(
                    application.getId(),
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.PAID,
                    actorId,
                    auditReason
            ));

            // Issue policy idempotently (delegating to PolicyService with OpenPDF schedule generation)
            Policy policy = null;
            try {
                if (policyService != null) {
                    policy = policyService.issuePolicy(application, payableAmount);
                } else {
                    policy = policyRepository.findByApplicationId(application.getId())
                            .orElseGet(() -> createPolicyForApplication(application, payableAmount));
                }
            } catch (Exception e) {
                log.error("Policy issuance failed for paid application {}: {}", applicationId, e.getMessage(), e);
                // Invariant (P3-05): Application status remains PAID; payment is never rolled back
            }

            String policyNo = policy != null ? policy.getPolicyNo() : "ISSUING_PENDING";
            log.info("Payment successful for application {}. Policy: {}", applicationId, policyNo);

            return PaymentResponse.success(
                    policyNo,
                    result.transactionReference(),
                    payableAmount,
                    result.processedAt() != null ? result.processedAt() : clock.instant()
            );

        } finally {
            activeApplicationPayments.remove(applicationId);
        }
    }

    private BigDecimal derivePayableAmount(Application application) {
        BigDecimal amount = null;
        if (application.getQuoteId() != null) {
            Optional<Quote> quoteOpt = quoteRepository.findById(application.getQuoteId());
            if (quoteOpt.isPresent() && quoteOpt.get().getPremium() != null) {
                amount = quoteOpt.get().getPremium();
            }
        }

        if (amount == null) {
            amount = application.getPremium();
        }

        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new ValidationException("Cannot derive a valid payable premium for application " + application.getId());
        }

        return amount;
    }

    private Policy createPolicyForApplication(Application application, BigDecimal premium) {
        Policy policy = new Policy();
        LocalDate today = LocalDate.now(clock);
        String policyNo = "POL-" + today.getYear() + "-" + String.format("%06d", application.getId());

        policy.setPolicyNo(policyNo);
        policy.setApplicationId(application.getId());
        policy.setCustomerId(application.getCustomerId());
        policy.setInsurerId(application.getInsurerId());
        policy.setPlanId(application.getPlanId());
        policy.setVehicleRegNo(application.getVehicleRegNo());
        policy.setPremiumPaid(premium);
        policy.setStartDate(today);
        policy.setEndDate(today.plusYears(1).minusDays(1));
        policy.setStatus(PolicyStatus.ACTIVE);

        return policyRepository.save(policy);
    }
}
