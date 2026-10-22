package com.team11.insurance.notifications.simulator;

/**
 * Exception representing messaging/notification delivery failure.
 */
public class NotificationDeliveryException extends Exception {
    private final boolean terminal;

    public NotificationDeliveryException(String message) {
        this(message, false);
    }

    public NotificationDeliveryException(String message, boolean terminal) {
        super(message);
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
