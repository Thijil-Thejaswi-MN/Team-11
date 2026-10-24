package com.team11.insurance.notifications.template;

/**
 * Thrown when required notification template parameters are missing or invalid (INS-F-016).
 */
public class NotificationValidationException extends RuntimeException {

    public NotificationValidationException(String message) {
        super(message);
    }

    public NotificationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
