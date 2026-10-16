package com.team11.insurance.policies;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;

/**
 * An issued policy (INS-F-009). Renewals create a NEW policy that points at the old one through
 * previousPolicyId and have no application (applicationId stays null).
 * UNIQUE(application_id) allows only one policy per application, which makes issuance idempotent.
 */
@Entity
@Table(name = "policies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_policy_no", columnNames = "policy_no"),
                @UniqueConstraint(name = "uq_policy_application", columnNames = "application_id")
        },
        indexes = {
                @Index(name = "idx_policy_customer", columnList = "customer_id"),
                @Index(name = "idx_policy_insurer_status", columnList = "insurer_id, status"),
                @Index(name = "idx_policy_expiry", columnList = "status, end_date"),
                @Index(name = "idx_policy_vehicle", columnList = "vehicle_reg_no, status")
        })
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_no", nullable = false, length = 30)
    private String policyNo;

    /** Null for renewals. */
    @Column(name = "application_id")
    private Long applicationId;

    /** Set on renewals: the policy this one renews. */
    @Column(name = "previous_policy_id")
    private Long previousPolicyId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "vehicle_reg_no", length = 20)
    private String vehicleRegNo;

    @Column(name = "premium_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal premiumPaid;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    /** Id returned by IFileStore.put for the policy PDF. */
    @Column(name = "document_file_id")
    private Long documentFileId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Policy() { }

    public Long getId() { return id; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getPreviousPolicyId() { return previousPolicyId; }
    public void setPreviousPolicyId(Long previousPolicyId) { this.previousPolicyId = previousPolicyId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getInsurerId() { return insurerId; }
    public void setInsurerId(Long insurerId) { this.insurerId = insurerId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public BigDecimal getPremiumPaid() { return premiumPaid; }
    public void setPremiumPaid(BigDecimal premiumPaid) { this.premiumPaid = premiumPaid; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }
    public Long getDocumentFileId() { return documentFileId; }
    public void setDocumentFileId(Long documentFileId) { this.documentFileId = documentFileId; }
    public Instant getCreatedAt() { return createdAt; }
}
