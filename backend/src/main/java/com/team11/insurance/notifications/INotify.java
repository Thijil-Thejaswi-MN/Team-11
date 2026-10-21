package com.team11.insurance.notifications;

import java.util.Map;

/**
 * Notification Service Interface (Component C9 - SAD Table 3.4b, Section 4.1.7 & ADR-005).
 * Consumed by: Quote & Purchase (C5), Claims Workflow (C7), Scheduler (C10).
 */
public interface INotify {

    /**
     * Canonical signature per SAD Table 3.4b.
     *
     * @param event           authoritative notification event from SAD Section 4.1.7
     * @param recipientUserId destination user primary key
     * @param params          dynamic template parameters (policyNo, claimNo, amount, etc.)
     */
    void send(NotificationEvent event, long recipientUserId, Map<String, String> params);

    /**
     * Overloaded DTO method supporting enriched routing (email, phone, channel).
     *
     * @param request structured notification request
     */
    void send(NotificationRequest request);
}
