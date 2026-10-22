package com.team11.insurance.payments;

import java.math.BigDecimal;

/**
 * Payment Adapter Interface (Component C8 - SAD Table 3.4b & ADR-007).
 * Consumed by: Quote & Purchase (C5/C7), Policy Renewal/Cancellation (C7), Claims Workflow (C7/C10).
 */
public interface IPaymentService {

    /**
     * Charges a customer for an approved application premium or renewal.
     *
     * @param applicationId  the ID of the insurance application being purchased
     * @param amount         monetary amount in INR (whole rupees, >= 1)
     * @param idempotencyKey server-generated key: "applicationId-attemptNo"
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     */
    PaymentResult charge(long applicationId, BigDecimal amount, String idempotencyKey);

    /**
     * Refunds a customer following an approved policy cancellation.
     *
     * @param policyId       the ID of the cancelled policy
     * @param amount         refund amount calculated per STP test oracle (Rs 0 skips gateway)
     * @param idempotencyKey server-generated key: "cancel-policyId-requestId"
     * @param reason         cancellation justification recorded for audit
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     */
    PaymentResult refund(long policyId, BigDecimal amount, String idempotencyKey, String reason);

    /**
     * Disburses an approved claim settlement to the customer.
     *
     * @param claimId            the ID of the approved claim
     * @param assessedAmount     the approved settlement amount in INR
     * @param idempotencyKey     server-generated key: "claim-claimId-payout"
     * @param destinationAccount masked bank account / UPI ID
     * @return PaymentResult indicating SUCCESS or FAILED outcome
     */
    PaymentResult payout(long claimId, BigDecimal assessedAmount, String idempotencyKey, String destinationAccount);

    /**
     * Queries payment status by idempotency key (SAD Table 3.4b getStatus).
     *
     * @param idempotencyKey the idempotency key of the payment attempt
     * @return PaymentResult for the attempt, or throws ResourceNotFoundException
     */
    PaymentResult getStatus(String idempotencyKey);
}
