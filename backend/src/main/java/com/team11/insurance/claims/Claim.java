package com.team11.insurance.claims;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Claim persistence entity (Component C7 - SAD §4.1.3, §4.3.5, SRS §4.7.1).
 */
@Entity
@Table(name = "claims",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_claim_no", columnNames = "claim_no")
        },
        indexes = {
                @Index(name = "idx_claim_policy", columnList = "policy_id"),
                @Index(name = "idx_claim_customer", columnList = "customer_id"),
                @Index(name = "idx_claim_insurer_status", columnList = "insurer_id, status")
        })
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_no", nullable = false, length = 30)
    private String claimNo;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "policy_no", length = 30)
    private String policyNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "claim_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal claimAmount;

    @Column(name = "assessed_amount", precision = 12, scale = 2)
    private BigDecimal assessedAmount;

    @Column(name = "settled_amount", precision = 12, scale = 2)
    private BigDecimal settledAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "duplicate_flag", nullable = false)
    private boolean duplicateFlag = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "claim_documents", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "document_file_id")
    private List<Long> documentIds = new ArrayList<>();

    @Column(name = "document_requested_at")
    private Instant documentRequestedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Claim() {
    }

    public Claim(Long id, String claimNo, Long policyId, String policyNo, Long customerId, Long insurerId,
                 LocalDate incidentDate, BigDecimal claimAmount, BigDecimal settledAmount,
                 ClaimStatus status, String reason) {
        this.id = id;
        this.claimNo = claimNo;
        this.policyId = policyId;
        this.policyNo = policyNo;
        this.customerId = customerId;
        this.insurerId = insurerId;
        this.incidentDate = incidentDate;
        this.claimAmount = claimAmount;
        this.settledAmount = settledAmount;
        this.status = status != null ? status : ClaimStatus.SUBMITTED;
        this.reason = reason;
        this.duplicateFlag = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public void setClaimNo(String claimNo) {
        this.claimNo = claimNo;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
    }

    public BigDecimal getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(BigDecimal claimAmount) {
        this.claimAmount = claimAmount;
    }

    public BigDecimal getAssessedAmount() {
        return assessedAmount;
    }

    public void setAssessedAmount(BigDecimal assessedAmount) {
        this.assessedAmount = assessedAmount;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isDuplicateFlag() {
        return duplicateFlag;
    }

    public void setDuplicateFlag(boolean duplicateFlag) {
        this.duplicateFlag = duplicateFlag;
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds != null ? new ArrayList<>(documentIds) : new ArrayList<>();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDocumentRequestedAt() {
        return documentRequestedAt;
    }

    public void setDocumentRequestedAt(Instant documentRequestedAt) {
        this.documentRequestedAt = documentRequestedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
