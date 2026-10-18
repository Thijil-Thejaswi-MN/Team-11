package com.team11.insurance.plans;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Plan persistence entity (Component C4 - SAD 3.4 & 4.3.2, INS-F-003, INS-F-004, INS-F-022).
 * <p>
 * Represents insurance plans with coverage, deductible, waiting period, base premium,
 * and associated rating and underwriting rules in JSON format.
 */
@Entity
@Table(name = "plans", indexes = {
        @Index(name = "idx_plan_insurer", columnList = "insurer_id"),
        @Index(name = "idx_plan_type", columnList = "type"),
        @Index(name = "idx_plan_code", columnList = "code"),
        @Index(name = "idx_plan_active", columnList = "is_active")
})
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "insurer_id", nullable = false)
    private Long insurerId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "coverage", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverage;

    @Column(name = "deductible", nullable = false, precision = 10, scale = 2)
    private BigDecimal deductible;

    @Column(name = "waiting_period_months", nullable = false)
    private Integer waitingPeriodMonths = 0;

    @Column(name = "base_premium", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePremium = BigDecimal.ZERO;

    @Column(name = "premium_rules_json", columnDefinition = "TEXT")
    private String premiumRulesJson;

    @Column(name = "underwriting_rules_json", columnDefinition = "TEXT")
    private String underwritingRulesJson;

    @Column(name = "add_ons_json", columnDefinition = "TEXT")
    private String addOnsJson;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Plan() {
    }

    public Plan(String code, Long insurerId, String type, String name,
                BigDecimal coverage, BigDecimal deductible, Integer waitingPeriodMonths,
                BigDecimal basePremium, String premiumRulesJson, String underwritingRulesJson,
                String addOnsJson, boolean active) {
        this.code = code;
        this.insurerId = insurerId;
        this.type = type;
        this.name = name;
        this.coverage = coverage;
        this.deductible = deductible;
        this.waitingPeriodMonths = waitingPeriodMonths != null ? waitingPeriodMonths : 0;
        this.basePremium = basePremium != null ? basePremium : BigDecimal.ZERO;
        this.premiumRulesJson = premiumRulesJson;
        this.underwritingRulesJson = underwritingRulesJson;
        this.addOnsJson = addOnsJson;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getInsurerId() {
        return insurerId;
    }

    public void setInsurerId(Long insurerId) {
        this.insurerId = insurerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCoverage() {
        return coverage;
    }

    public void setCoverage(BigDecimal coverage) {
        this.coverage = coverage;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public Integer getWaitingPeriodMonths() {
        return waitingPeriodMonths;
    }

    public void setWaitingPeriodMonths(Integer waitingPeriodMonths) {
        this.waitingPeriodMonths = waitingPeriodMonths;
    }

    public BigDecimal getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(BigDecimal basePremium) {
        this.basePremium = basePremium;
    }

    public String getPremiumRulesJson() {
        return premiumRulesJson;
    }

    public void setPremiumRulesJson(String premiumRulesJson) {
        this.premiumRulesJson = premiumRulesJson;
    }

    public String getUnderwritingRulesJson() {
        return underwritingRulesJson;
    }

    public void setUnderwritingRulesJson(String underwritingRulesJson) {
        this.underwritingRulesJson = underwritingRulesJson;
    }

    public String getAddOnsJson() {
        return addOnsJson;
    }

    public void setAddOnsJson(String addOnsJson) {
        this.addOnsJson = addOnsJson;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
