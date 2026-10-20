package com.team11.insurance.kyc.entity;

import com.team11.insurance.kyc.KycStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Persistent KYC record for customer verification (INS-F-002, INS-SR-007).
 */
@Entity
@Table(name = "kyc_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_kyc_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_kyc_user", columnList = "user_id"),
                @Index(name = "idx_kyc_status", columnList = "status")
        })
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Convert(converter = com.team11.insurance.common.crypto.EncryptedStringConverter.class)
    @Column(name = "pan_number", length = 512)
    private String panNumber;

    @Convert(converter = com.team11.insurance.common.crypto.EncryptedStringConverter.class)
    @Column(name = "aadhaar_number", length = 512)
    private String aadhaarNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycStatus status = KycStatus.PENDING;

    @Column(name = "document_path", length = 255)
    private String documentPath;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KycRecord() {
    }

    public KycRecord(Long userId, String panNumber, String aadhaarNumber, KycStatus status) {
        this.userId = userId;
        this.panNumber = panNumber;
        this.aadhaarNumber = aadhaarNumber;
        this.status = status != null ? status : KycStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    public KycStatus getStatus() {
        return status;
    }

    public void setStatus(KycStatus status) {
        this.status = status;
        if (status == KycStatus.VERIFIED) {
            this.verifiedAt = Instant.now();
        }
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "KycRecord{" +
                "id=" + id +
                ", userId=" + userId +
                ", panNumber='" + com.team11.insurance.common.logging.SensitiveDataMasker.maskPan(panNumber) + '\'' +
                ", aadhaarNumber='" + com.team11.insurance.common.logging.SensitiveDataMasker.maskAadhaar(aadhaarNumber) + '\'' +
                ", status=" + status +
                ", documentPath='" + documentPath + '\'' +
                ", verifiedAt=" + verifiedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
