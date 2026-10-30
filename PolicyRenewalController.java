package com.team11.insurance.policies;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.auth.CurrentUser;
import com.team11.insurance.auth.UserRepository;
import com.team11.insurance.auth.UserRole;
import com.team11.insurance.claims.IClaimService;
import com.team11.insurance.common.exception.AccessDeniedException;
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
 * Policy Renewal REST Controller (P3-09 / INS-F-019 / INS-F-024).
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyRenewalController {

    private final PolicyRepository policyRepository;
    private final PolicyPdfGenerator policyPdfGenerator;
    private final IFileStore fileStore;
    private final QuoteRepository quoteRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final IClaimService claimService;
    private final IPaymentService paymentService;
    private final INotify notifyService;
    private final IAuditLog auditLog;
    private final Clock clock;

    public PolicyRenewalController(
            PolicyRepository policyRepository,
            PolicyPdfGenerator policyPdfGenerator,
            IFileStore fileStore,
            ObjectProvider<QuoteRepository> quoteRepositoryProvider,
            ObjectProvider<PlanRepository> planRepositoryProvider,
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<IClaimService> claimServiceProvider,
            ObjectProvider<IPaymentService> paymentServiceProvider,
            ObjectProvider<INotify> notifyServiceProvider,
            ObjectProvider<IAuditLog> auditLogProvider,
            ObjectProvider<Clock> clockProvider) {
        this.policyRepository = policyRepository;
        this.policyPdfGenerator = policyPdfGenerator;
        this.fileStore = fileStore;
        this.quoteRepository = quoteRepositoryProvider.getIfAvailable();
        this.planRepository = planRepositoryProvider.getIfAvailable();
        this.userRepository = userRepositoryProvider.getIfAvailable();
        this.claimService = claimServiceProvider.getIfAvailable();
        this.paymentService = paymentServiceProvider.getIfAvailable();
        this.notifyService = notifyServiceProvider.getIfAvailable();
        this.auditLog = auditLogProvider.getIfAvailable();
        Clock providedClock = clockProvider.getIfAvailable();
        this.clock = providedClock != null ? providedClock : Clock.systemDefaultZone();
    }

    @PostMapping("/{id}/renewal-quote")
    @Transactional
    public ResponseEntity<RenewalQuoteResponse> createRenewalQuote(
            @PathVariable Long id,
            CurrentUser currentUser) {
        requireCustomer(currentUser);

        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
        verifyOwnership(policy, currentUser);

        if (policy.getStatus() != PolicyStatus.ACTIVE && policy.getStatus() != PolicyStatus.EXPIRED) {
            throw new StateConflictException("Cancelled or pending-cancellation policies cannot be renewed.");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = policy.getEndDate().minusDays(30);
        LocalDate windowEnd = policy.getEndDate().plusDays(15);
        if (today.isBefore(windowStart) || today.isAfter(windowEnd)) {
            throw new StateConflictException(
                    "Renewal is only allowed within 30 days before to 15 days after policy expiry ("
                            + windowStart + " to " + windowEnd + ").");
        }

        boolean hadSettledClaim = claimService != null && claimService.hasPaidClaim(id);

        Plan plan = (planRepository != null && policy.getPlanId() != null)
                ? planRepository.findById(policy.getPlanId()).orElse(null)
                : null;

        BigDecimal currentIdv = (plan != null && plan.getSumInsured() != null && plan.getSumInsured().compareTo(BigDecimal.ZERO) > 0)
                ? plan.getSumInsured()
                : policy.getPremiumPaid().multiply(BigDecimal.valueOf(50));

        String rawFormula = (plan != null && plan.getFormulaExpression() != null && !plan.getFormulaExpression().isBlank())
                ? plan.getFormulaExpression()
                : "OD=2%; AGE=5-10:5%; DEP=10%";
        MotorRatingParameters params = MotorRatingParametersParser.parse(rawFormula);

        LocalDate manufactureDate = policy.getStartDate().minusYears(5);
        MotorRenewalQuote calc = RenewalQuoteCalculator.calculateMotorRenewal(
                currentIdv,
                manufactureDate,
                policy.getEndDate(),
                hadSettledClaim,
                List.of(),
                params
        );

        Long savedQuoteId = null;
        if (quoteRepository != null) {
            Quote renewalQuote = new Quote(
                    currentUser.getUserId(),
                    policy.getPlanId(),
                    calc.newIdv(),
                    calc.baseOdPremium(),
                    calc.ageLoading(),
                    BigDecimal.ZERO,
                    calc.addOnsTotal(),
                    calc.ncbDiscount(),
                    calc.renewalPremium(),
                    "{}",
                    calc.renewableUntilDate()
            );
            Quote saved = quoteRepository.save(renewalQuote);
            if (saved != null) {
                savedQuoteId = saved.getId();
            }
        }

        return ResponseEntity.ok(new RenewalQuoteResponse(
                savedQuoteId,
                id,
                calc.renewalPremium(),
                calc.newIdv(),
                calc.ncbDiscount(),
                calc.newTermStartDate(),
                calc.newTermStartDate().plusYears(1).minusDays(1)
        ));
    }

    @PostMapping("/{id}/renew")
    @Transactional
    public ResponseEntity<RenewalResponse> renewPolicy(
            @PathVariable Long id,
            @RequestBody(required = false) RenewalConfirmRequest request,
            CurrentUser currentUser) {
        requireCustomer(currentUser);

        RenewalQuoteResponse quoteRes = createRenewalQuote(id, currentUser).getBody();
        if (quoteRes == null) {
            throw new StateConflictException("Unable to compute renewal quote for policy " + id);
        }

        Policy oldPolicy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));

        BigDecimal payable = quoteRes.premium();
        if (paymentService != null) {
            String idemKey = "renew-" + id + "-" + (quoteRes.quoteId() != null ? quoteRes.quoteId() : LocalDate.now(clock));
            PaymentResult payResult = paymentService.charge(id, payable, idemKey);
            if (payResult != null && payResult.status() == PaymentStatus.FAILED) {
                throw new StateConflictException("Renewal payment failed: " + payResult.failureReason());
            }
        }

        String newPolicyNo = generateUniquePolicyNo(quoteRes.newStartDate().getYear());
        Policy renewed = new Policy();
        renewed.setPolicyNo(newPolicyNo);
        renewed.setApplicationId(null);
        renewed.setCustomerId(oldPolicy.getCustomerId());
        renewed.setInsurerId(oldPolicy.getInsurerId());
        renewed.setPlanId(oldPolicy.getPlanId());
        renewed.setVehicleRegNo(oldPolicy.getVehicleRegNo());
        renewed.setPremiumPaid(payable);
        renewed.setStartDate(quoteRes.newStartDate());
        renewed.setEndDate(quoteRes.newEndDate());
        renewed.setStatus(PolicyStatus.ACTIVE);
        renewed.setPreviousPolicyId(id);

        if (policyPdfGenerator != null && fileStore != null) {
            byte[] pdfBytes = policyPdfGenerator.generatePolicyPdf(renewed, "Renewal Policy Schedule", "Customer #" + oldPolicy.getCustomerId());
            StoredDocument stored = fileStore.storeStream(
                    new ByteArrayInputStream(pdfBytes),
                    "policy-" + newPolicyNo + ".pdf",
                    "application/pdf",
                    pdfBytes.length,
                    DocumentCategory.POLICY,
                    oldPolicy.getCustomerId()
            );
            if (stored != null && stored.id() != null) {
                renewed.setDocumentFileId(stored.id());
            }
        }

        Policy savedPolicy = policyRepository.save(renewed);

        if (auditLog != null) {
            auditLog.record(currentUser.getUserId(), "POLICY_RENEWED", "Policy",
                    savedPolicy != null && savedPolicy.getId() != null ? savedPolicy.getId() : id,
                    "Renewed from policyId=" + id + ", policyNo=" + newPolicyNo);
        }

        if (notifyService != null) {
            notifyService.send(NotificationEvent.POLICY_RENEWED, oldPolicy.getCustomerId(), Map.of(
                    "policyNo", newPolicyNo,
                    "amount", payable.toPlainString()
            ));
        }

        return ResponseEntity.ok(new RenewalResponse(
                savedPolicy != null ? savedPolicy.getId() : null,
                newPolicyNo,
                PolicyStatus.ACTIVE,
                quoteRes.newStartDate(),
                quoteRes.newEndDate(),
                payable
        ));
    }

    private String generateUniquePolicyNo(int year) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int rand = (int) (Math.abs(UUID.randomUUID().getMostSignificantBits()) % 90000) + 10000;
            String candidate = String.format("POL-%d-%05d", year, rand);
            if (!policyRepository.existsByPolicyNo(candidate)) {
                return candidate;
            }
        }
        return "POL-" + year + "-" + (System.currentTimeMillis() % 100000);
    }

    private void requireCustomer(CurrentUser currentUser) {
        if (currentUser == null || currentUser.getUserRole() == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        if (currentUser.getUserRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("Only customers can renew policies.");
        }
    }

    private void verifyOwnership(Policy policy, CurrentUser currentUser) {
        if (!Objects.equals(policy.getCustomerId(), currentUser.getUserId())) {
            throw new AccessDeniedException("You do not have permission to renew this policy.");
        }
    }

    public record RenewalQuoteResponse(
            Long quoteId,
            Long policyId,
            BigDecimal premium,
            BigDecimal newIdv,
            BigDecimal ncbDiscount,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) {}

    public record RenewalConfirmRequest(Long quoteId) {}

    public record RenewalResponse(
            Long policyId,
            String policyNo,
            PolicyStatus status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal premiumPaid
    ) {}
}
