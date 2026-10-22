package com.team11.insurance.notifications;

/**
 * Authoritative 16-Event Notification Catalogue (SAD §4.1.7).
 */
public enum NotificationEvent {
    // 1. Quotes & Underwriting (C5/C6)
    APPLICATION_APPROVED,      // Underwriting approves, or Underwriter manually approves
    APPLICATION_REFERRED,      // Underwriting gives Refer
    APPLICATION_REJECTED,      // Underwriting or Underwriter rejects (with reason)

    // 2. Payments (C8)
    PAYMENT_FAILED,            // A premium payment attempt fails (retry allowed)

    // 3. Policies & Renewals (C5/C10)
    POLICY_ISSUED,             // Policy becomes Active after payment
    POLICY_RENEWED,            // Renewal policy is issued
    RENEWAL_REMINDER,          // Triggered by Scheduler at 30, 15, and 1 day before expiry

    // 4. Cancellations (C5)
    CANCELLATION_REQUESTED,    // Customer requests cancellation
    CANCELLATION_APPROVED,     // Insurer Admin approves; refund amount included
    CANCELLATION_REJECTED,     // Insurer Admin rejects

    // 5. Claims Workflow (C7)
    CLAIM_RECEIVED,            // Claim is filed (Submitted, then Document Verification)
    CLAIM_DOCUMENTS_REQUESTED, // Claims Officer requests additional documents
    CLAIM_APPROVED,            // Claim approved (moves to Payment Processing)
    CLAIM_REJECTED,            // Claim rejected (with reason)
    CLAIM_SETTLED,             // Claim payout succeeded
    CLAIM_CLOSED               // No reply to a document request for 15 days
}
