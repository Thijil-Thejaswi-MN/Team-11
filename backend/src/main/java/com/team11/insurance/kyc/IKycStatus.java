package com.team11.insurance.kyc;

/**
 * Shared service interface for querying KYC verification status across modules (INS-F-002).
 * <p>
 * Used by Policy issuance, Underwriting, and Application processing to verify customer eligibility.
 */
public interface IKycStatus {

    /**
     * Checks if the specified user has completed and passed KYC verification.
     *
     * @param userId customer user ID
     * @return true if the customer has a verified KYC record, false otherwise
     */
    boolean isVerified(long userId);

    /**
     * Returns the current KYC status of the customer.
     *
     * @param userId customer user ID
     * @return the {@link KycStatus} (PENDING, VERIFIED, REJECTED, or NOT_SUBMITTED)
     */
    KycStatus getStatus(long userId);
}
