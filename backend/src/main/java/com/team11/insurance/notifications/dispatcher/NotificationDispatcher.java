package com.team11.insurance.notifications.dispatcher;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team11.insurance.notifications.NotificationChannel;
import com.team11.insurance.notifications.NotificationOutbox;
import com.team11.insurance.notifications.NotificationOutboxRepository;
import com.team11.insurance.notifications.NotificationStatus;
import com.team11.insurance.notifications.retry.NotificationRetryPolicy;
import com.team11.insurance.notifications.sender.INotificationChannelSender;
import com.team11.insurance.notifications.simulator.NotificationDeliveryException;
import com.team11.insurance.notifications.template.NotificationPayload;
import com.team11.insurance.notifications.template.NotificationTemplateService;
import com.team11.insurance.notifications.template.NotificationValidationException;

/**
 * Dispatcher handling atomic claim, template rendering, channel delivery,
 * multi-channel fallback, retry scheduling, and dead-letter failure logging (P4-08).
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationTemplateService templateService;
    private final INotificationChannelSender channelSender;
    private final NotificationRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationDispatcher(
            NotificationOutboxRepository outboxRepository,
            NotificationTemplateService templateService,
            INotificationChannelSender channelSender,
            NotificationRetryPolicy retryPolicy,
            ObjectMapper objectMapper,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.templateService = templateService;
        this.channelSender = channelSender;
        this.retryPolicy = retryPolicy;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Atomically claims and dispatches a single notification outbox record.
     * Executes in a separate transaction (REQUIRES_NEW) to maintain isolated delivery state.
     *
     * @param outboxId primary key of the notification outbox row
     * @return true if claimed and delivered, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean dispatch(Long outboxId) {
        if (outboxId == null) {
            return false;
        }

        Instant now = clock.instant();

        // 1. Atomic claim: transition PENDING -> PROCESSING to prevent race conditions & duplicate sends
        int claimed = outboxRepository.updateStatusAtomically(outboxId, NotificationStatus.PENDING, NotificationStatus.PROCESSING, now);
        if (claimed == 0) {
            log.debug("Outbox row [{}] already claimed or not in PENDING state; skipping duplicate dispatch.", outboxId);
            return false;
        }

        NotificationOutbox outbox = outboxRepository.findById(outboxId).orElse(null);
        if (outbox == null) {
            log.error("Outbox record [{}] claimed but not found in database.", outboxId);
            return false;
        }

        // Increment attempt count
        outbox.setAttemptCount(outbox.getAttemptCount() + 1);

        // Parse template params
        Map<String, String> params = parseParams(outbox.getParamsJson());

        // 2. Validate and render templates
        NotificationPayload payload;
        try {
            payload = templateService.render(outbox.getEvent(), params);
            outbox.setRenderedSubject(payload.subject());
            outbox.setRenderedBody(outbox.getChannel() == NotificationChannel.SMS ? payload.smsBody() : payload.emailBody());
        } catch (NotificationValidationException e) {
            log.error("DEAD-LETTER LOG: Template validation failed for notification [id={} event={}]: {}",
                    outboxId, outbox.getEvent(), e.getMessage());
            outbox.setStatus(NotificationStatus.FAILED);
            outbox.setFailureReason("Validation error: " + e.getMessage());
            outbox.setLastError(e.getMessage());
            outbox.setNextRetryAt(null);
            outbox.setUpdatedAt(clock.instant());
            outboxRepository.save(outbox);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error rendering template for notification [id={}]: {}", outboxId, e.getMessage(), e);
            outbox.setStatus(NotificationStatus.FAILED);
            outbox.setFailureReason("Template render error: " + e.getMessage());
            outbox.setLastError(e.getMessage());
            outbox.setNextRetryAt(null);
            outbox.setUpdatedAt(clock.instant());
            outboxRepository.save(outbox);
            return false;
        }

        // 3. Deliver across channels with fallback
        try {
            DeliveryResult deliveryResult = deliverPayload(outbox, payload);

            // Successful delivery (clean, partial, or fallback)
            outbox.setStatus(NotificationStatus.SENT);
            outbox.setSentAt(clock.instant());
            outbox.setFailureReason(deliveryResult.failureReason());
            outbox.setLastError(deliveryResult.lastError());
            outbox.setNextRetryAt(null);
            outbox.setUpdatedAt(clock.instant());
            outboxRepository.save(outbox);

            if (deliveryResult.failureReason() != null) {
                log.warn("Notification [id={}] event=[{}] SENT with delivery caveat: {}",
                        outboxId, outbox.getEvent(), deliveryResult.failureReason());
            } else {
                log.info("Notification [id={}] event=[{}] successfully SENT via channel [{}]",
                        outboxId, outbox.getEvent(), outbox.getChannel());
            }
            return true;
        } catch (Exception e) {
            handleDeliveryFailure(outbox, e);
            return false;
        }
    }

    private record DeliveryResult(String failureReason, String lastError) {}

    private DeliveryResult deliverPayload(NotificationOutbox outbox, NotificationPayload payload) throws NotificationDeliveryException {
        NotificationChannel channel = outbox.getChannel() != null ? outbox.getChannel() : NotificationChannel.EMAIL;
        String email = outbox.getRecipientEmail();
        String phone = outbox.getRecipientPhone();

        switch (channel) {
            case EMAIL -> {
                try {
                    if (email == null || email.isBlank()) {
                        throw new NotificationDeliveryException("Recipient email address is missing", true);
                    }
                    channelSender.sendEmail(email, payload.subject(), payload.emailBody());
                    return new DeliveryResult(null, null);
                } catch (NotificationDeliveryException e) {
                    // Multi-channel fallback: if phone number is available, attempt SMS delivery
                    if (phone != null && !phone.isBlank()) {
                        log.warn("Email delivery failed for notification [{}]: {}. Attempting fallback to SMS [{}]",
                                outbox.getId(), e.getMessage(), maskPhone(phone));
                        try {
                            channelSender.sendSms(phone, payload.smsBody());
                            log.info("Notification [{}] successfully delivered via SMS fallback.", outbox.getId());
                            String warning = "FALLBACK_DELIVERY: Primary Email delivery failed (" + e.getMessage() + "); delivered via SMS fallback to " + maskPhone(phone);
                            return new DeliveryResult(warning, e.getMessage());
                        } catch (NotificationDeliveryException smsEx) {
                            throw new NotificationDeliveryException("Email failed (" + e.getMessage() + ") and SMS fallback also failed (" + smsEx.getMessage() + ")", smsEx.isTerminal());
                        }
                    } else {
                        throw e;
                    }
                }
            }
            case SMS -> {
                try {
                    if (phone == null || phone.isBlank()) {
                        throw new NotificationDeliveryException("Recipient phone number is missing", true);
                    }
                    channelSender.sendSms(phone, payload.smsBody());
                    return new DeliveryResult(null, null);
                } catch (NotificationDeliveryException e) {
                    // Multi-channel fallback: if email is available, attempt Email delivery
                    if (email != null && !email.isBlank()) {
                        log.warn("SMS delivery failed for notification [{}]: {}. Attempting fallback to Email [{}]",
                                outbox.getId(), e.getMessage(), maskEmail(email));
                        try {
                            channelSender.sendEmail(email, payload.subject(), payload.emailBody());
                            log.info("Notification [{}] successfully delivered via Email fallback.", outbox.getId());
                            String warning = "FALLBACK_DELIVERY: Primary SMS delivery failed (" + e.getMessage() + "); delivered via Email fallback to " + maskEmail(email);
                            return new DeliveryResult(warning, e.getMessage());
                        } catch (NotificationDeliveryException emailEx) {
                            throw new NotificationDeliveryException("SMS failed (" + e.getMessage() + ") and Email fallback also failed (" + emailEx.getMessage() + ")", emailEx.isTerminal());
                        }
                    } else {
                        throw e;
                    }
                }
            }
            case ALL -> {
                boolean emailSent = false;
                boolean smsSent = false;
                NotificationDeliveryException emailEx = null;
                NotificationDeliveryException smsEx = null;

                if (email != null && !email.isBlank()) {
                    try {
                        emailSent = channelSender.sendEmail(email, payload.subject(), payload.emailBody());
                    } catch (NotificationDeliveryException e) {
                        emailEx = e;
                        log.warn("ALL routing: Email delivery failed for notification [{}]: {}", outbox.getId(), e.getMessage());
                    }
                }

                if (phone != null && !phone.isBlank()) {
                    try {
                        smsSent = channelSender.sendSms(phone, payload.smsBody());
                    } catch (NotificationDeliveryException e) {
                        smsEx = e;
                        log.warn("ALL routing: SMS delivery failed for notification [{}]: {}", outbox.getId(), e.getMessage());
                    }
                }

                if (!emailSent && !smsSent) {
                    if (emailEx != null || smsEx != null) {
                        String msg = "ALL delivery failed across all channels." +
                                (emailEx != null ? " Email: " + emailEx.getMessage() : "") +
                                (smsEx != null ? " SMS: " + smsEx.getMessage() : "");
                        boolean terminal = (emailEx != null && emailEx.isTerminal()) && (smsEx != null && smsEx.isTerminal());
                        throw new NotificationDeliveryException(msg, terminal);
                    }
                    throw new NotificationDeliveryException("Neither email nor phone destination was provided for ALL channel", true);
                }

                if (emailSent && !smsSent) {
                    String reason = "PARTIAL_DELIVERY: Email delivered successfully; SMS delivery failed: " + (smsEx != null ? smsEx.getMessage() : "recipient phone missing or invalid");
                    String lastError = smsEx != null ? smsEx.getMessage() : "SMS delivery failed";
                    return new DeliveryResult(reason, lastError);
                } else if (!emailSent && smsSent) {
                    String reason = "PARTIAL_DELIVERY: SMS delivered successfully; Email delivery failed: " + (emailEx != null ? emailEx.getMessage() : "recipient email missing or invalid");
                    String lastError = emailEx != null ? emailEx.getMessage() : "Email delivery failed";
                    return new DeliveryResult(reason, lastError);
                } else {
                    return new DeliveryResult(null, null);
                }
            }
            default -> throw new NotificationDeliveryException("Unsupported notification channel: " + channel, true);
        }
    }

    private void handleDeliveryFailure(NotificationOutbox outbox, Exception e) {
        boolean isTerminal = false;
        if (e instanceof NotificationDeliveryException nde) {
            isTerminal = nde.isTerminal();
        }

        boolean shouldRetry = retryPolicy.shouldRetry(outbox.getAttemptCount(), isTerminal);
        Instant now = clock.instant();

        if (shouldRetry) {
            Instant nextRetry = retryPolicy.calculateNextRetry(outbox.getAttemptCount(), now);
            outbox.setStatus(NotificationStatus.PENDING);
            outbox.setNextRetryAt(nextRetry);
            outbox.setFailureReason("Transient failure: " + e.getMessage());
            outbox.setLastError(e.getMessage());
            outbox.setUpdatedAt(now);
            outboxRepository.save(outbox);
            log.warn("Notification [id={}] failed attempt {}. Retrying at {} (delay {}s). Error: {}",
                    outbox.getId(), outbox.getAttemptCount(), nextRetry,
                    retryPolicy.getBackoffSeconds(outbox.getAttemptCount()), e.getMessage());
        } else {
            outbox.setStatus(NotificationStatus.FAILED);
            outbox.setNextRetryAt(null);
            outbox.setFailureReason(isTerminal
                    ? "Terminal failure: " + e.getMessage()
                    : "Exceeded max retries (" + retryPolicy.getMaxRetries() + "): " + e.getMessage());
            outbox.setLastError(e.getMessage());
            outbox.setUpdatedAt(now);
            outboxRepository.save(outbox);
            log.error("DEAD-LETTER LOG: Notification [id={}] permanently FAILED after {} attempt(s). RecipientUserId=[{}] Event=[{}] Reason: {}",
                    outbox.getId(), outbox.getAttemptCount(), outbox.getRecipientUserId(), outbox.getEvent(), outbox.getFailureReason());
        }
    }

    private Map<String, String> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize notification outbox paramsJson: {}", json, e);
            return Collections.emptyMap();
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return (at > 2 ? email.substring(0, 2) : email.substring(0, 1)) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
