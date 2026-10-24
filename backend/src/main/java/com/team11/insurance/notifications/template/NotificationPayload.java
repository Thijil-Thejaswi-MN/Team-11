package com.team11.insurance.notifications.template;

/**
 * Rendered message payload containing subject, email body, and SMS message.
 */
public record NotificationPayload(
        String subject,
        String emailBody,
        String smsBody
) {
}
