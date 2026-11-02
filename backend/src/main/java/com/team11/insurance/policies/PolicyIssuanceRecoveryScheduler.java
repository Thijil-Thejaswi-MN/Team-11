package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
 */
@Component
public class PolicyIssuanceRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PolicyIssuanceRecoveryScheduler.class);

    private final ApplicationRepository applicationRepository;
    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final QuoteRepository quoteRepository;
    private final IAuditLog auditLog;

    public PolicyIssuanceRecoveryScheduler(
            ApplicationRepository applicationRepository,
            PolicyRepository policyRepository,
            PolicyService policyService,
            ObjectProvider<QuoteRepository> quoteRepositoryProvider,
            ObjectProvider<IAuditLog> auditLogProvider) {
        this.applicationRepository = applicationRepository;
        this.policyRepository = policyRepository;
        this.policyService = policyService;
        this.quoteRepository = quoteRepositoryProvider.getIfAvailable();
        this.auditLog = auditLogProvider.getIfAvailable();
    }

    @Scheduled(cron = "${insurance.policy.issuance-recovery.cron:0 0 3 * * ?}")
    public int runIssuanceRecoveryJob() {
        return retryPendingIssuance();
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
                recoveredCount++;
                if (auditLog != null && issued != null) {
                    auditLog.recordSystem(
                            "POLICY_ISSUANCE_RECOVERED",
                            "Application",
                            app.getId(),
                            "Recovered policy issuance for paid application id=" + app.getId()
                                    + ", policyNo=" + issued.getPolicyNo()
                    );
                }
            } catch (Exception ex) {
                log.warn("Delayed policy issuance retry failed for applicationId={}: {}",
                        app.getId(), ex.getMessage());
            }
        }
        return recoveredCount;
    }

    private BigDecimal resolvePremium(Application app) {
        if (app.getPremium() != null && app.getPremium().compareTo(BigDecimal.ZERO) > 0) {
            return app.getPremium();
        }
        if (quoteRepository != null && app.getQuoteId() != null) {
            Quote quote = quoteRepository.findById(app.getQuoteId()).orElse(null);
            if (quote != null && quote.getPremium() != null && quote.getPremium().compareTo(BigDecimal.ZERO) > 0) {
                return quote.getPremium();
            }
        }
        return BigDecimal.ONE;
    }
}
