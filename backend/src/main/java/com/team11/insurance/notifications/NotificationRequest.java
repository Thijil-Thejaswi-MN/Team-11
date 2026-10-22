package com.team11.insurance.notifications;

import java.util.Map;

/**
 * Structured Notification Request DTO (SAD §4.1.7).
 */
public record NotificationRequest(
    NotificationEvent event,
    Long recipientUserId,
    String recipientEmail,
    String recipientPhone,
    NotificationChannel channel,    // EMAIL, SMS, ALL
    Map<String, String> params,
    String correlationId
) { }
