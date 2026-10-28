package com.team11.insurance.claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduled background worker for automated stale claim auto-closure (Task D, P1-19, TC-Clm-06, SAD §4.1.3, §4.3.5).
 * Periodically sweeps claims remaining in the Document Verification lifecycle state where the customer
 * has failed to supply requested documentation after the statutory 15-day grace period.
 * Automatically transitions stale claims to CLOSED_NO_RESPONSE and emits the CLAIM_CLOSED notification event.
 */
@Component
public class ClaimAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClaimAutoCloseScheduler.class);

    private final IClaimService claimService;

    public ClaimAutoCloseScheduler(IClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * Daily auto-close job running at 02:00 AM server time (configurable via insurance.claims.auto-close.cron).
     *
     * @return count of claims transitioned to Closed - No Response
     */
    @Scheduled(cron = "${insurance.claims.auto-close.cron:0 0 2 * * ?}")
    public int runAutoCloseJob() {
        log.info("Starting daily scheduled claim auto-close sweep for stale document requests...");
        int closedCount = claimService.closeStale();
        log.info("Claim auto-close sweep completed: {} stale claim(s) transitioned to Closed - No Response.", closedCount);
        return closedCount;
    }
}
