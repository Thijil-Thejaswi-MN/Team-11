package com.team11.insurance.policies;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.RoleSecurity;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.claims.IClaimService;
import com.team11.insurance.common.exception.AccessDeniedException;
import com.team11.insurance.common.exception.PaymentFailedException;
import com.team11.insurance.common.exception.ResourceNotFoundException;
import com.team11.insurance.common.exception.StateConflictException;
import com.team11.insurance.filestore.DocumentCategory;
import com.team11.insurance.filestore.IFileStore;
import com.team11.insurance.filestore.StoredDocument;
import com.team11.insurance.notifications.INotify;
import com.team11.insurance.notifications.NotificationEvent;
import com.team11.insurance.payments.IPaymentService;
import com.team11.insurance.payments.PaymentResult;
import com.team11.insurance.payments.PaymentStatus;
import com.team11.insurance.plans.Plan;
import com.team11.insurance.plans.PlanRepository;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

/**
 * REST endpoints and workflow for Policy Renewal (P3-09 / INS-F-019, INS-F-024).
 */
@RestController
@RequestMapping("/policies")
public class PolicyRenewalController {

    private static final Logger log = LoggerFactory.getLogger(PolicyRenewalController.class);
    private static final String DEFAULT_MOTOR_RULES = "OD=2%; AGE=5-10:5%; DEP=10%";

    private final PolicyRepository policyRepository;
    private final PolicyPdfGenerator policyPdfGenerator;
    private final IFileStore fileStore;
    private final ObjectProvider<QuoteRepository> quoteRepositoryProvider;
    private final ObjectProvider<PlanRepository> planRepositoryProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<IClaimService> claimServiceProvider;
    private final ObjectProvider<IPaymentService> paymentServiceProvider;
    private final ObjectProvider<INotify> notifyProvider;
    private final ObjectProvider<IAuditLog> auditLogProvider;
    private final Clock clock;

    @Autowired
    public PolicyRenewalController(
            PolicyRepository policyRepository,
            PolicyPdfGenerator policyPdfGenerator,
            IFileStore fileStore,
            ObjectProvider<QuoteRepository> quoteRepositoryProvider,
            ObjectProvider<PlanRepository> planRepositoryProvider,
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<IClaimService> claimServiceProvider,
            ObjectProvider<IPaymentService> paymentServiceProvider,
            ObjectProvider<INotify> notifyProvider,
            ObjectProvider<IAuditLog> auditLogProvider,
            ObjectProvider<Clock> clockProvider) {
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
        this.policyPdfGenerator = Objects.requireNonNull(policyPdfGenerator, "policyPdfGenerator must not be null");
        this.fileStore = Objects.requireNonNull(fileStore, "fileStore must not be null");
        this.quoteRepositoryProvider = quoteRepositoryProvider;
        this.planRepositoryProvider = planRepositoryProvider;
        this.userRepositoryProvider = userRepositoryProvider;
        this.claimServiceProvider = claimServiceProvider;
        this.paymentServiceProvider = paymentServiceProvider;
        this.notifyProvider = notifyProvider;
        this.auditLogProvider = auditLogProvider;
        this.clock = clockProvider != null && clockProvider.getIfAvailable() != null
                ? clockProvider.getIfAvailable()
                : Clock.systemUTC();
    }

    public record RenewalQuoteResponse(
            Long renewalQuoteId,
            Long policyId,
            BigDecimal premium,
            BigDecimal newIdv,
            BigDecimal ncbDiscount,
            boolean claimPaidInTerm,
            LocalDate newTermStartDate,
            LocalDate newTermEndDate,
            LocalDate renewableUntil,
            Map<String, Object> breakdown
    ) {}

    public record RenewalConfirmRequest(Long renewalQuoteId) {}

    public record RenewalResponse(
            Long newPolicyId,
            String newPolicyNo,
            Long previousPolicyId,
            BigDecimal premiumPaid,
            LocalDate startDate,
            LocalDate endDate,
            PolicyStatus status,
            Long documentFileId
    ) {}

    @PostMapping("/{id}/renewal-quote")
    @Transactional
    public ResponseEntity<RenewalQuoteResponse> createRenewalQuote(
            @PathVariable Long id,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);
        Policy policy = loadOwnedPolicy(id, currentUser);

        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.CANCELLATION_REQUESTED) {
            throw new StateConflictException("Cancelled policies cannot be renewed.");
        }

        LocalDate today = LocalDate.now(clock);
        if (!RenewalQuoteCalculator.isWithinRenewalWindow(policy.getEndDate(), today)) {
            throw new StateConflictException("Policy is outside the renewal window (30 days before to 15 days after expiry).");
        }

        Plan plan = resolvePlan(policy.getPlanId());
        if (plan != null && !plan.isActive()) {
            throw new StateConflictException("The plan associated with this policy is no longer active for renewal.");
        }

        boolean claimPaid = hasSettledClaim(policy.getId());
        MotorRenewalQuote calc = computeQuote(policy, plan, claimPaid);

        Long renewalQuoteId = null;
        QuoteRepository quoteRepo = quoteRepositoryProvider != null ? quoteRepositoryProvider.getIfAvailable() : null;
        if (quoteRepo != null) {
            String productType = plan != null && plan.getType() != null ? plan.getType() : "MOTOR";
            String breakdownJson = String.format(
                    "{\"newIdv\":%s,\"baseOdPremium\":%s,\"ageLoading\":%s,\"ncbDiscount\":%s,\"addOnsTotal\":%s,\"renewalPremium\":%s}",
                    calc.newIdv().toPlainString(),
                    calc.baseOdPremium().toPlainString(),
                    calc.ageLoading().toPlainString(),
                    calc.ncbDiscount().toPlainString(),
                    calc.addOnsTotal().toPlainString(),
                    calc.renewalPremium().toPlainString()
            );
            Quote quote = new Quote(
                    policy.getCustomerId(),
                    policy.getInsurerId(),
                    policy.getPlanId(),
                    productType,
                    calc.renewalPremium(),
                    breakdownJson,
                    "{\"renewalForPolicyId\":" + policy.getId() + "}",
                    Instant.now(clock).plusSeconds(15 * 86400L)
            );
            Quote saved = quoteRepo.save(quote);
            renewalQuoteId = saved.getId();
        }

        Map<String, Object> breakdown = Map.of(
                "newIdv", calc.newIdv(),
                "baseOdPremium", calc.baseOdPremium(),
                "ageLoading", calc.ageLoading(),
                "ncbDiscount", calc.ncbDiscount(),
                "addOnsTotal", calc.addOnsTotal(),
                "renewalPremium", calc.renewalPremium()
        );

        return ResponseEntity.ok(new RenewalQuoteResponse(
                renewalQuoteId != null ? renewalQuoteId : policy.getId(),
                policy.getId(),
                calc.renewalPremium(),
                calc.newIdv(),
                calc.ncbDiscount(),
                claimPaid,
                calc.newTermStartDate(),
                calc.newTermEndDate(),
                calc.renewableUntilDate(),
                breakdown
        ));
    }

    @PostMapping("/{id}/renew")
    @Transactional
    public ResponseEntity<RenewalResponse> renewPolicy(
            @PathVariable Long id,
            @RequestBody(required = false) RenewalConfirmRequest request,
            CurrentUser currentUser) {
        RoleSecurity.assertRole(currentUser, UserRole.CUSTOMER);
        Policy policy = loadOwnedPolicy(id, currentUser);

        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.CANCELLATION_REQUESTED) {
            throw new StateConflictException("Cancelled policies cannot be renewed.");
        }

        LocalDate today = LocalDate.now(clock);
        if (!RenewalQuoteCalculator.isWithinRenewalWindow(policy.getEndDate(), today)) {
            throw new StateConflictException("Policy is outside the renewal window (30 days before to 15 days after expiry).");
        }

        boolean alreadyRenewed = policyRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .anyMatch(p -> Objects.equals(p.getPreviousPolicyId(), policy.getId()));
        if (alreadyRenewed) {
            throw new StateConflictException("This policy has already been renewed.");
        }

        Plan plan = resolvePlan(policy.getPlanId());
        if (plan != null && !plan.isActive()) {
            throw new StateConflictException("The plan associated with this policy is no longer active for renewal.");
        }

        BigDecimal renewalPremium;
        Long quoteIdForKey = request != null ? request.renewalQuoteId() : null;
        QuoteRepository quoteRepo = quoteRepositoryProvider != null ? quoteRepositoryProvider.getIfAvailable() : null;

        if (quoteIdForKey != null && quoteRepo != null) {
            Quote quote = quoteRepo.findById(quoteIdForKey)
                    .orElseThrow(() -> new ResourceNotFoundException("Renewal quote not found: " + quoteIdForKey));
            if (!Objects.equals(quote.getCustomerId(), currentUser.getUserId())) {
                throw new AccessDeniedException("You are not authorized to use this renewal quote.");
            }
            if (!Objects.equals(quote.getPlanId(), policy.getPlanId())) {
                throw new StateConflictException("Renewal quote does not match policy plan.");
            }
            renewalPremium = quote.getPremium();
        } else {
            boolean claimPaid = hasSettledClaim(policy.getId());
            renewalPremium = computeQuote(policy, plan, claimPaid).renewalPremium();
        }

        IPaymentService paymentService = paymentServiceProvider != null ? paymentServiceProvider.getIfAvailable() : null;
        if (paymentService != null) {
            String idempotencyKey = "renew-" + policy.getId() + "-" + (quoteIdForKey != null ? quoteIdForKey : "direct");
            PaymentResult paymentResult = paymentService.charge(policy.getId(), renewalPremium, idempotencyKey);
            if (paymentResult != null && paymentResult.status() == PaymentStatus.FAILED) {
                throw new PaymentFailedException(
                        paymentResult.failureReason() != null ? paymentResult.failureReason() : "Renewal payment failed"
                );
            }
        }

        LocalDate newStart = policy.getEndDate().plusDays(1);
        LocalDate newEnd = newStart.plusYears(1).minusDays(1);
        String newPolicyNo = generateUniquePolicyNo(newStart.getYear());

        Policy renewed = new Policy();
        renewed.setPolicyNo(newPolicyNo);
        renewed.setPreviousPolicyId(policy.getId());
        renewed.setCustomerId(policy.getCustomerId());
        renewed.setInsurerId(policy.getInsurerId());
        renewed.setPlanId(policy.getPlanId());
        renewed.setVehicleRegNo(policy.getVehicleRegNo());
        renewed.setPremiumPaid(renewalPremium);
        renewed.setStartDate(newStart);
        renewed.setEndDate(newEnd);
        renewed.setStatus(PolicyStatus.ACTIVE);

        try {
            String planName = plan != null && plan.getName() != null ? plan.getName() : "Plan #" + policy.getPlanId();
            String customerName = resolveCustomerName(policy.getCustomerId());
            byte[] pdfBytes = policyPdfGenerator.generatePolicyPdf(renewed, planName, customerName);
            StoredDocument stored = fileStore.storeStream(
                    new ByteArrayInputStream(pdfBytes),
                    "policy-" + newPolicyNo + ".pdf",
                    "application/pdf",
                    pdfBytes.length,
                    DocumentCategory.POLICY,
                    policy.getCustomerId()
            );
            if (stored != null && stored.id() != null) {
                renewed.setDocumentFileId(stored.id());
            }
        } catch (Exception ex) {
            log.warn("Renewal PDF generation failed for policy {}: {}", newPolicyNo, ex.getMessage());
        }

        Policy saved = policyRepository.save(renewed);

        INotify notify = notifyProvider != null ? notifyProvider.getIfAvailable() : null;
        if (notify != null) {
            notify.send(
                    NotificationEvent.POLICY_RENEWED,
                    policy.getCustomerId(),
                    Map.of(
                            "policyNo", saved.getPolicyNo(),
                            "previousPolicyNo", policy.getPolicyNo(),
                            "premium", renewalPremium.toPlainString()
                    )
            );
        }

        IAuditLog auditLog = auditLogProvider != null ? auditLogProvider.getIfAvailable() : null;
        if (auditLog != null && saved.getId() != null) {
            auditLog.record(
                    currentUser.getUserId(),
                    "POLICY_RENEWED",
                    "Policy",
                    saved.getId(),
                    "Renewed from policy " + policy.getPolicyNo()
            );
        }

        return ResponseEntity.ok(new RenewalResponse(
                saved.getId(),
                saved.getPolicyNo(),
                saved.getPreviousPolicyId(),
                saved.getPremiumPaid(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getStatus(),
                saved.getDocumentFileId()
        ));
    }

    private Policy loadOwnedPolicy(Long id, CurrentUser currentUser) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
        if (!Objects.equals(policy.getCustomerId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You are not authorized to access this policy.");
        }
        return policy;
    }

    private boolean hasSettledClaim(Long policyId) {
        IClaimService claimService = claimServiceProvider != null ? claimServiceProvider.getIfAvailable() : null;
        return claimService != null && claimService.hasPaidClaim(policyId);
    }

    private Plan resolvePlan(Long planId) {
        PlanRepository planRepo = planRepositoryProvider != null ? planRepositoryProvider.getIfAvailable() : null;
        if (planRepo == null || planId == null) {
            return null;
        }
        return planRepo.findById(planId).orElse(null);
    }

    private String resolveCustomerName(Long customerId) {
        UserRepository userRepo = userRepositoryProvider != null ? userRepositoryProvider.getIfAvailable() : null;
        if (userRepo == null || customerId == null) {
            return "Customer #" + customerId;
        }
        return userRepo.findById(customerId).map(User::getFullName).orElse("Customer #" + customerId);
    }

    private MotorRenewalQuote computeQuote(Policy policy, Plan plan, boolean claimPaid) {
        BigDecimal currentIdv = (plan != null && plan.getCoverage() != null && plan.getCoverage().compareTo(BigDecimal.ZERO) > 0)
                ? plan.getCoverage()
                : policy.getPremiumPaid().multiply(new BigDecimal("50"));
        LocalDate manufactureDate = policy.getStartDate() != null
                ? policy.getStartDate().minusYears(5)
                : LocalDate.now(clock).minusYears(5);
        MotorRatingParameters params = resolveRatingParameters(plan);
        return RenewalQuoteCalculator.calculateMotorRenewal(
                currentIdv,
                manufactureDate,
                policy.getEndDate(),
                claimPaid,
                List.of(),
                params
        );
    }

    private MotorRatingParameters resolveRatingParameters(Plan plan) {
        if (plan != null && plan.getPremiumRulesJson() != null && plan.getPremiumRulesJson().contains("OD=")) {
            try {
                return MotorRatingParametersParser.parse(plan.getPremiumRulesJson());
            } catch (Exception ignored) {
            }
        }
        return MotorRatingParametersParser.parse(DEFAULT_MOTOR_RULES);
    }

    private String generateUniquePolicyNo(int year) {
        for (int attempt = 0; attempt < 10; attempt++) {
            long suffix = Math.abs((System.nanoTime() + attempt) % 100000L);
            String candidate = String.format("POL-%d-%05d", year, suffix);
            if (!policyRepository.existsByPolicyNo(candidate)) {
                return candidate;
            }
        }
        return "POL-" + year + "-" + System.currentTimeMillis();
    }
}
