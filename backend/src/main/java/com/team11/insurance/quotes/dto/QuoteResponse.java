package com.team11.insurance.quotes.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload for motor and health quote calculations (API Contract §2.1, SAD §4.3.3).
 */
public class QuoteResponse {

    @JsonProperty("quoteId")
    private Long quoteId;

    @JsonProperty("premium")
    private BigDecimal premium;

    @JsonProperty("breakdown")
    private PremiumBreakdown breakdown;

    @JsonProperty("validUntil")
    private Instant validUntil;

    public QuoteResponse() {
    }

    public QuoteResponse(Long quoteId, BigDecimal premium, PremiumBreakdown breakdown, Instant validUntil) {
        this.quoteId = quoteId;
        this.premium = premium;
        this.breakdown = breakdown;
        this.validUntil = validUntil;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public PremiumBreakdown getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(PremiumBreakdown breakdown) {
        this.breakdown = breakdown;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }
}
