package com.team11.insurance.applications;

import java.math.BigDecimal;
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

import jakarta.persistence.UniqueConstraint;

/**
 * An insurance application (INS-F-007). Columns that point at other modules (customer, insurer,
 * plan, quote) are plain ids on purpose: no foreign keys until those modules' tables are merged.
 */
@Entity
@Table(name = "applications", indexes = {
        @Index(name = "idx_app_customer", columnList = "customer_id"),
        @Index(name = "idx_app_insurer_status", columnList = "insurer_id, status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_application_quote", columnNames = "quote_id")
})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "quote_id", nullable = false, unique = true)
    private Long quoteId;

    /** TWO_WHEELER, FOUR_WHEELER or HEALTH. A String until the quotes module fixes a shared enum. */
    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    /** Motor only. PROPOSED in the contract, used by existsActivePolicyForVehicle. */
    @Column(name = "vehicle_reg_no", length = 20)
    private String vehicleRegNo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal premium;

    /** Free-form application details as JSON text. */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Ids of the uploaded supporting documents, comma separated (for example "301,302"). */
    @Column(name = "document_ids", length = 500)
    private String documentIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Application() { }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getInsurerId() { return insurerId; }
    public void setInsurerId(Long insurerId) { this.insurerId = insurerId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getDocumentIds() { return documentIds; }
    public void setDocumentIds(String documentIds) { this.documentIds = documentIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
