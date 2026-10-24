package com.team11.insurance.notifications;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.auth.entity.User;
import com.team11.insurance.auth.repository.UserRepository;
import com.team11.insurance.notifications.dispatcher.NotificationAsyncService;

/**
 * Production-ready, durable Asynchronous Notification Service implementing {@link INotify} (P4-08).
 * <p>
 * Enqueues immutable delivery records into {@code notification_outbox} (status: PENDING)
 * and triggers asynchronous dispatch via {@link NotificationAsyncService} upon caller transaction commit.
 * <p>
 * Strictly guarantees that notification dispatch latency, slow gateways, or delivery failures
 * never block or roll back the calling business operation (SAD §4.1.7, ADR-005).
 */
@Service
@Primary
public class NotificationService implements INotify {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationAsyncService asyncService;
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationService(
            NotificationOutboxRepository outboxRepository,
            NotificationAsyncService asyncService,
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectMapper objectMapper,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.asyncService = asyncService;
        this.userRepositoryProvider = userRepositoryProvider;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void send(NotificationEvent event, long recipientUserId, Map<String, String> params) {
        if (event == null) {
            log.error("Attempted to send notification with null event for userId: {}", recipientUserId);
            return;
        }

        NotificationRequest request = new NotificationRequest(
                event,
                recipientUserId,
                null,
                null,
                NotificationChannel.EMAIL,
                params,
                null
        );
        send(request);
    }

    @Override
    public void send(NotificationRequest request) {
        if (request == null) {
            log.error("Attempted to send null NotificationRequest.");
            return;
        }
        if (request.event() == null) {
            log.error("NotificationRequest has null event for userId: {}", request.recipientUserId());
            return;
        }

        try {
            Instant now = clock.instant();

            // 1. Resolve contact details from UserRepository if missing
            String email = request.recipientEmail();
            String phone = request.recipientPhone();

            if ((email == null || phone == null) && request.recipientUserId() != null) {
                UserRepository userRepo = userRepositoryProvider.getIfAvailable();
                if (userRepo != null) {
                    try {
                        User user = userRepo.findById(request.recipientUserId()).orElse(null);
                        if (user != null) {
                            if (email == null) {
                                email = user.getEmail();
                            }
                            if (phone == null) {
                                phone = user.getPhone();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Unable to look up user [{}] for notification routing: {}",
                                request.recipientUserId(), e.getMessage());
                    }
                }
            }

            // 2. Serialize parameters
            String paramsJson = serializeParams(request.params());

            // 3. Persist outbox record
            NotificationOutbox outbox = new NotificationOutbox();
            outbox.setEvent(request.event());
            outbox.setRecipientUserId(request.recipientUserId());
            outbox.setRecipientEmail(email);
            outbox.setRecipientPhone(phone);
            outbox.setChannel(request.channel() != null ? request.channel() : NotificationChannel.EMAIL);
            outbox.setParamsJson(paramsJson);
            outbox.setCorrelationId(request.correlationId());
            outbox.setStatus(NotificationStatus.PENDING);
            outbox.setAttemptCount(0);
            outbox.setCreatedAt(now);
            outbox.setUpdatedAt(now);

            outbox = outboxRepository.save(outbox);
            final Long outboxId = outbox.getId();

            log.info("Enqueued notification outbox [id={}] event=[{}] recipientUserId=[{}] channel=[{}] correlationId=[{}]",
                    outboxId, request.event(), request.recipientUserId(), outbox.getChannel(), request.correlationId());

            // 4. Trigger asynchronous dispatch after caller transaction commits (or immediately if non-transactional)
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncService.dispatchAsync(outboxId);
                    }
                });
            } else {
                asyncService.dispatchAsync(outboxId);
            }
        } catch (Throwable t) {
            // Guardrail: Notification enqueueing errors MUST NOT crash the calling business operation
            log.error("CRITICAL: Failed to enqueue notification outbox for event [{}] userId [{}]: {}",
                    request.event(), request.recipientUserId(), t.getMessage(), t);
        }
    }

    private String serializeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize notification params to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
