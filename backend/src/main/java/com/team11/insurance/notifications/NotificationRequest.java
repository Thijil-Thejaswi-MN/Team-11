package com.team11.insurance.notifications;

import java.util.Map;

/**
 * Structured Notification Request DTO (Component C9 - SAD Table 3.4b, Section 4.1.7).
 */
public record NotificationRequest(
        NotificationEvent event,
        Long recipientUserId,
        String recipientEmail,
        String recipientPhone,
        NotificationChannel channel,
        Map<String, String> params,
        String correlationId
) {
    public NotificationRequest {
        if (event == null) {
            throw new IllegalArgumentException("NotificationEvent cannot be null");
        }
        params = params != null ? Map.copyOf(params) : Map.of();
    }
}
