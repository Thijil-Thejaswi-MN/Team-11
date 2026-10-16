package com.team11.insurance.applications;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** One row per status change of an application (SAD ADR-008: history records every transition). */
@Entity
@Table(name = "application_history", indexes = {
        @Index(name = "idx_apphist_app", columnList = "application_id")
})
public class ApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** Null for the first row (creation). */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private ApplicationStatus toStatus;

    /** Null means the system made the change. */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ApplicationHistory() { }

    public ApplicationHistory(Long applicationId, ApplicationStatus fromStatus,
                              ApplicationStatus toStatus, Long actorId, String reason) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public Long getActorId() { return actorId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
