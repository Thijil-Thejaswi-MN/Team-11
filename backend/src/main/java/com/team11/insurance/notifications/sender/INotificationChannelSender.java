package com.team11.insurance.notifications.sender;

import com.team11.insurance.notifications.NotificationChannel;
import com.team11.insurance.notifications.simulator.NotificationDeliveryException;

/**
 * Pluggable abstraction for channel-specific notification delivery (SAD §4.1.7).
 * Decouples outbox dispatcher logic from specific email/SMS gateway SDKs.
 */
public interface INotificationChannelSender {

    /**
     * Dispatches an email message to the specified recipient.
     *
     * @param toEmail destination email address
     * @param subject rendered email subject
     * @param body    rendered email content
     * @return true if successfully delivered
     * @throws NotificationDeliveryException if delivery fails (transient or terminal)
     */
    boolean sendEmail(String toEmail, String subject, String body) throws NotificationDeliveryException;

    /**
     * Dispatches an SMS message to the specified recipient phone number.
     *
     * @param toPhone destination phone number (e.g. E.164 format)
     * @param message rendered SMS body
     * @return true if successfully delivered
     * @throws NotificationDeliveryException if delivery fails (transient or terminal)
     */
    boolean sendSms(String toPhone, String message) throws NotificationDeliveryException;

    /**
     * Checks if the sender supports the specified channel.
     *
     * @param channel delivery channel (EMAIL, SMS, ALL)
     * @return true if the channel is supported
     */
    default boolean supports(NotificationChannel channel) {
        return channel != null;
    }
}
