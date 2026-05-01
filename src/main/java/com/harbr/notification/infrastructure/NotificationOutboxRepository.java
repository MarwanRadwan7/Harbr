package com.harbr.notification.infrastructure;

import com.harbr.notification.domain.NotificationOutbox;
import com.harbr.notification.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    List<NotificationOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}