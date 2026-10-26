package com.team11.insurance.notifications.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.team11.insurance.notifications.simulator.NotificationDeliveryException;

/**
 * Production fallback {@link INotificationChannelSender} when no external provider (e.g. SES, SendGrid, Twilio)
 * is bound. Strictly avoids pretending delivery succeeded and does not introduce hardcoded credentials.
 */
@Component
@Profile({"prod", "default"})
public class DefaultNotificationChannelSender implements INotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationChannelSender.class);

    @Override
    public boolean sendEmail(String toEmail, String subject, String body) throws NotificationDeliveryException {
        log.warn("Production Email provider not configured. Delivery to [{}] cannot proceed without external gateway configuration.",
                maskEmail(toEmail));
        throw new NotificationDeliveryException("Production Email provider not configured. Configure external gateway via environment.", true);
    }

    @Override
    public boolean sendSms(String toPhone, String message) throws NotificationDeliveryException {
        log.warn("Production SMS provider not configured. Delivery to [{}] cannot proceed without external gateway configuration.",
                maskPhone(toPhone));
        throw new NotificationDeliveryException("Production SMS provider not configured. Configure external gateway via environment.", true);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return (at > 2 ? email.substring(0, 2) : email.substring(0, 1)) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
