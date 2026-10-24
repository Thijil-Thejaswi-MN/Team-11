package com.team11.insurance.notifications.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous dispatcher service executing off-thread on {@code notificationExecutor} (SAD §4.1.7).
 * Ensures delivery latency, connection delays, or provider outages never block the calling transaction.
 */
@Service
public class NotificationAsyncService {

    private static final Logger log = LoggerFactory.getLogger(NotificationAsyncService.class);

    private final NotificationDispatcher dispatcher;

    public NotificationAsyncService(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Executes notification dispatch asynchronously in the {@code notificationExecutor} pool.
     *
     * @param outboxId primary key of the pending outbox record
     */
    @Async("notificationExecutor")
    public void dispatchAsync(Long outboxId) {
        log.debug("Entering async notification dispatch for outboxId [{}] on thread [{}]",
                outboxId, Thread.currentThread().getName());
        try {
            dispatcher.dispatch(outboxId);
        } catch (Throwable t) {
            log.error("Unhandled error during async notification dispatch for outboxId [{}]: {}",
                    outboxId, t.getMessage(), t);
        }
    }
}
