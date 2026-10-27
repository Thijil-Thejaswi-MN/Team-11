package com.team11.insurance.notifications;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link NotificationOutbox} entities.
 */
@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findByStatus(NotificationStatus status);

    List<NotificationOutbox> findByRecipientUserId(Long recipientUserId);

    List<NotificationOutbox> findByEvent(NotificationEvent event);

    List<NotificationOutbox> findByCorrelationId(String correlationId);

    @Query("SELECT n FROM NotificationOutbox n WHERE n.status = 'PENDING' AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now) ORDER BY n.createdAt ASC")
    List<NotificationOutbox> findPendingDueForDelivery(@Param("now") Instant now);

    /**
     * Atomically claims an outbox record from PENDING to PROCESSING state.
     * Prevents race conditions and duplicate delivery when multiple threads or scheduler ticks execute concurrently.
     *
     * @param id the notification outbox ID
     * @param fromStatus current state expected (typically PENDING)
     * @param toStatus desired state (typically PROCESSING)
     * @param now timestamp for updatedAt
     * @return 1 if successfully transitioned; 0 if already claimed/transitioned by another worker
     */
    @Modifying
    @Query("UPDATE NotificationOutbox n SET n.status = :toStatus, n.updatedAt = :now WHERE n.id = :id AND n.status = :fromStatus")
    int updateStatusAtomically(
            @Param("id") Long id,
            @Param("fromStatus") NotificationStatus fromStatus,
            @Param("toStatus") NotificationStatus toStatus,
            @Param("now") Instant now
    );
}
