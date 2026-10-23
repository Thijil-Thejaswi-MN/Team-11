package com.team11.insurance.notifications;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * In-memory development and test stub for {@link INotify} (Work Item: P4-06).
 * <p>
 * Captures and logs all notification dispatches without sending real Emails or SMS messages.
 * Provides thread-safe inspection helpers for test assertions.
 * <p>
 * Strictly active under {@code dev} and {@code test} profiles.
 */
@Service
@Profile({"dev", "test"})
public class NotificationServiceStub implements INotify {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceStub.class);

    private final List<NotificationRequest> capturedNotifications = new CopyOnWriteArrayList<>();

    @Override
    public void send(NotificationEvent event, long recipientUserId, Map<String, String> params) {
        if (event == null) {
            throw new IllegalArgumentException("NotificationEvent cannot be null");
        }
        log.info("NotificationServiceStub: Sending event [{}] to recipientUserId=[{}] with params={}",
                event, recipientUserId, params);

        NotificationRequest request = new NotificationRequest(
                event,
                recipientUserId,
                null,
                null,
                NotificationChannel.EMAIL,
                params,
                null
        );
        capturedNotifications.add(request);
    }

    @Override
    public void send(NotificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("NotificationRequest cannot be null");
        }
        log.info("NotificationServiceStub: Sending structured request event [{}] to recipientUserId=[{}] channel=[{}]",
                request.event(), request.recipientUserId(), request.channel());

        capturedNotifications.add(request);
    }

    // =========================================================================
    // Test & Diagnostic Helpers
    // =========================================================================

    public List<NotificationRequest> getCapturedNotifications() {
        return List.copyOf(capturedNotifications);
    }

    public List<NotificationRequest> getNotificationsForUser(long userId) {
        return capturedNotifications.stream()
                .filter(n -> Objects.equals(n.recipientUserId(), userId))
                .collect(Collectors.toList());
    }

    public List<NotificationRequest> getNotificationsForEvent(NotificationEvent event) {
        return capturedNotifications.stream()
                .filter(n -> n.event() == event)
                .collect(Collectors.toList());
    }

    public void clear() {
        capturedNotifications.clear();
        log.info("NotificationServiceStub: Cleared captured notifications.");
    }

    public int count() {
        return capturedNotifications.size();
    }
}
