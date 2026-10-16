package com.team11.insurance.policies;

import java.math.BigDecimal;
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

/** A customer's request to cancel a policy (INS-F-020), decided by the Insurer Admin. */
@Entity
@Table(name = "cancellation_requests", indexes = {
        @Index(name = "idx_cancel_policy", columnList = "policy_id"),
        @Index(name = "idx_cancel_insurer_status", columnList = "insurer_id, status")
})
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    /** Copied from the policy so insurer staff queries can be scoped (SAD ADR-009). */
    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CancellationStatus status = CancellationStatus.REQUESTED;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CancellationRequest() { }

    public Long getId() { return id; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public Long getInsurerId() { return insurerId; }
    public void setInsurerId(Long insurerId) { this.insurerId = insurerId; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public CancellationStatus getStatus() { return status; }
    public void setStatus(CancellationStatus status) { this.status = status; }
    public Long getDecidedBy() { return decidedBy; }
    public void setDecidedBy(Long decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
