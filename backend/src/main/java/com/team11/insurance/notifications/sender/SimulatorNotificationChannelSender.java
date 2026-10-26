package com.team11.insurance.notifications.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.team11.insurance.notifications.simulator.NotificationDeliveryException;
import com.team11.insurance.notifications.simulator.NotificationSenderSimulator;

/**
 * Adapter integrating {@link NotificationSenderSimulator} as an active {@link INotificationChannelSender}
 * for development and testing environments (P4-05 & P4-08).
 */
@Component
@Profile({"dev", "test"})
public class SimulatorNotificationChannelSender implements INotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(SimulatorNotificationChannelSender.class);

    private final NotificationSenderSimulator simulator;

    public SimulatorNotificationChannelSender(NotificationSenderSimulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public boolean sendEmail(String toEmail, String subject, String body) throws NotificationDeliveryException {
        log.debug("Simulating Email delivery to [{}]: subject='{}'", toEmail, subject);
        return simulator.simulateDelivery(toEmail, "EMAIL", toEmail);
    }

    @Override
    public boolean sendSms(String toPhone, String message) throws NotificationDeliveryException {
        log.debug("Simulating SMS delivery to [{}]: message='{}'", toPhone, message);
        return simulator.simulateDelivery(toPhone, "SMS", toPhone);
    }

    public NotificationSenderSimulator getSimulator() {
        return simulator;
    }
}
