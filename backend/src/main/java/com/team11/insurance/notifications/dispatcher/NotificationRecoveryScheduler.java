package com.team11.insurance.notifications.dispatcher;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.team11.insurance.notifications.NotificationOutbox;
import com.team11.insurance.notifications.NotificationOutboxRepository;

/**
 * Scheduled background worker recovering pending notifications and retrying backoff work (P4-08).
 * Ensures delivery survives application crashes/restarts and scheduled retries execute when due.
 */
@Component
public class NotificationRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecoveryScheduler.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDispatcher dispatcher;
    private final Clock clock;

    public NotificationRecoveryScheduler(
            NotificationOutboxRepository outboxRepository,
            NotificationDispatcher dispatcher,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /**
     * Periodic poller recovering pending outbox records that are due for delivery.
     */
    @Scheduled(fixedDelayString = "${app.notifications.recovery.fixed-delay-ms:5000}")
    public void pollPendingNotifications() {
        triggerRecovery();
    }

    /**
     * Programmatic trigger for testing and administrative sweeps.
     *
     * @return number of notifications dispatched
     */
    public int triggerRecovery() {
        List<NotificationOutbox> dueList = outboxRepository.findPendingDueForDelivery(clock.instant());
        if (dueList.isEmpty()) {
            return 0;
        }

        log.debug("Notification recovery poller found {} pending notification(s) due for delivery.", dueList.size());
        int dispatchedCount = 0;
        for (NotificationOutbox outbox : dueList) {
            try {
                boolean result = dispatcher.dispatch(outbox.getId());
                if (result) {
                    dispatchedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing pending notification [id={}]: {}", outbox.getId(), e.getMessage(), e);
            }
        }
        return dispatchedCount;
    }
}
