package com.team11.insurance.notifications;

/**
 * Authoritative 16-Event Notification Catalogue (Component C9 - SAD Table 3.4b, Section 4.1.7, ADR-005).
 */
public enum NotificationEvent {
    // 1. Quotes & Underwriting (C5/C6)
    APPLICATION_APPROVED,
    APPLICATION_REFERRED,
    APPLICATION_REJECTED,

    // 2. Payments (C8)
    PAYMENT_FAILED,

    // 3. Policies & Renewals (C5/C10)
    POLICY_ISSUED,
    POLICY_RENEWED,
    RENEWAL_REMINDER,

    // 4. Cancellations (C5)
    CANCELLATION_REQUESTED,
    CANCELLATION_APPROVED,
    CANCELLATION_REJECTED,

    // 5. Claims Workflow (C7)
    CLAIM_RECEIVED,
    CLAIM_DOCUMENTS_REQUESTED,
    CLAIM_APPROVED,
    CLAIM_REJECTED,
    CLAIM_SETTLED,
    CLAIM_CLOSED
}
