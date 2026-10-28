package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.team11.insurance.admin.IAuditLog;
import com.team11.insurance.applications.Application;
import com.team11.insurance.applications.ApplicationRepository;
import com.team11.insurance.applications.ApplicationStatus;
import com.team11.insurance.quotes.Quote;
import com.team11.insurance.quotes.QuoteRepository;

/**
 * Scheduled background recovery job for delayed policy issuance (P3-12 / INS-F-009, SAD 4.1.2).
 * Retries policy generation for applications that completed payment (status = PAID)
 * but whose initial synchronous policy issuance or PDF storage failed.
 */
@Component
public class PolicyIssuanceRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PolicyIssuanceRecoveryScheduler.class);

    private final ApplicationRepository applicationRepository;
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final ObjectProvider<QuoteRepository> quoteRepositoryProvider;
    private final ObjectProvider<IAuditLog> auditLogProvider;

    @Autowired
    public PolicyIssuanceRecoveryScheduler(
            ApplicationRepository applicationRepository,
            PolicyRepository policyRepository,
            PolicyService policyService,
            ObjectProvider<QuoteRepository> quoteRepositoryProvider,
            ObjectProvider<IAuditLog> auditLogProvider) {
        this.applicationRepository = Objects.requireNonNull(applicationRepository, "applicationRepository must not be null");
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
        this.policyService = Objects.requireNonNull(policyService, "policyService must not be null");
        this.quoteRepositoryProvider = quoteRepositoryProvider;
        this.auditLogProvider = auditLogProvider;
    }

    @Scheduled(cron = "${insurance.policy.issuance-recovery.cron:0 0 3 * * ?}")
    public int runIssuanceRecoveryJob() {
        log.info("Starting scheduled policy issuance recovery sweep for PAID applications...");
        int recovered = retryPendingIssuance();
        log.info("Policy issuance recovery sweep completed: {} policy(ies) issued.", recovered);
        return recovered;
    }

    public int retryPendingIssuance() {
        List<Application> paidApplications = applicationRepository.findByStatus(ApplicationStatus.PAID);
        int recoveredCount = 0;

        for (Application app : paidApplications) {
            if (app == null || app.getId() == null) {
                continue;
            }
            if (policyRepository.existsByApplicationId(app.getId())) {
                continue;
            }
            try {
                BigDecimal premium = resolvePremium(app);
                Policy issued = policyService.issuePolicy(app, premium);
                if (issued != null) {
                    recoveredCount++;
                    IAuditLog auditLog = auditLogProvider != null ? auditLogProvider.getIfAvailable() : null;
                    if (auditLog != null && issued.getId() != null) {
                        auditLog.recordSystem(
                                "POLICY_ISSUANCE_RECOVERED",
                                "Policy",
                                issued.getId(),
                                "Recovered policy issuance for paid application " + app.getId()
                        );
                    }
                }
            } catch (Exception ex) {
                log.warn("Issuance recovery retry failed for paid application {}: {}", app.getId(), ex.getMessage());
            }
        }
        return recoveredCount;
    }

    private BigDecimal resolvePremium(Application app) {
        if (app.getPremium() != null && app.getPremium().compareTo(BigDecimal.ZERO) > 0) {
            return app.getPremium();
        }
        QuoteRepository quoteRepo = quoteRepositoryProvider != null ? quoteRepositoryProvider.getIfAvailable() : null;
        if (quoteRepo != null && app.getQuoteId() != null) {
            return quoteRepo.findById(app.getQuoteId())
                    .map(Quote::getPremium)
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(BigDecimal.ONE);
        }
        return BigDecimal.ONE;
    }
}
