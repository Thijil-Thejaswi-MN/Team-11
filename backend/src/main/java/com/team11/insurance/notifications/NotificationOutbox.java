package com.team11.insurance.notifications;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Persistent transactional outbox entity for asynchronous multi-channel notification dispatch
 * (Component C9 - SAD Table 3.4b, §4.1.7; ADR-005; P4 Contract Section 4).
 * <p>
 * Ensures business operations enqueue notifications safely within their local database
 * transaction without waiting for or being rolled back by third-party SMTP/SMS network latency.
 */
@Entity
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_outbox_recipient", columnList = "recipient_user_id"),
        @Index(name = "idx_outbox_correlation", columnList = "correlation_id")
})
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 40)
    private NotificationEvent event;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "rendered_subject", length = 255)
    private String renderedSubject;

    @Column(name = "rendered_body", columnDefinition = "TEXT")
    private String renderedBody;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationOutbox() {
    }

    public NotificationOutbox(NotificationEvent event,
                              Long recipientUserId,
                              String recipientEmail,
                              String recipientPhone,
                              NotificationChannel channel,
                              String paramsJson,
                              String correlationId) {
        this.event = event;
        this.recipientUserId = recipientUserId;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.channel = channel != null ? channel : NotificationChannel.EMAIL;
        this.paramsJson = paramsJson;
        this.correlationId = correlationId;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public NotificationEvent getEvent() {
        return event;
    }

    public void setEvent(NotificationEvent event) {
        this.event = event;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public void setRenderedSubject(String renderedSubject) {
        this.renderedSubject = renderedSubject;
    }

    public String getRenderedBody() {
        return renderedBody;
    }

    public void setRenderedBody(String renderedBody) {
        this.renderedBody = renderedBody;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
