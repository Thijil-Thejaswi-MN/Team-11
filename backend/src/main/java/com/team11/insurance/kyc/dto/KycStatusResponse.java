package com.team11.insurance.kyc.dto;

/**
 * KYC status response DTO (API Contract §3.2).
 */
public class KycStatusResponse {

    private String kycStatus;

    public KycStatusResponse() {
    }

    public KycStatusResponse(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }
}
