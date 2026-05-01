package com.harbr.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harbr.notification.application.dto.NotificationResponse;
import com.harbr.notification.domain.Notification;
import com.harbr.notification.domain.NotificationChannel;
import com.harbr.notification.domain.NotificationOutbox;
import com.harbr.notification.domain.OutboxStatus;
import com.harbr.notification.infrastructure.NotificationOutboxRepository;
import com.harbr.notification.infrastructure.NotificationRepository;
import com.harbr.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createNotification(UUID userId, String title, String message,
                                   NotificationChannel channel, String eventType,
                                   String aggregateType, String aggregateId,
                                   UUID relatedEntityId, String relatedEntityType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .channel(channel)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build();

        notification = notificationRepository.save(notification);

        OutboxEventPayload payload = new OutboxEventPayload(
                notification.getId(), userId, title, message, channel.name(), relatedEntityType, relatedEntityId
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for notification {}", notification.getId(), e);
        }

        log.info("Notification created: id={}, userId={}, channel={}", notification.getId(), userId, channel);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toNotificationResponse);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new com.harbr.common.exception.ForbiddenException("You can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return toNotificationResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        Page<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 100));

        unread.stream()
                .filter(n -> !n.getIsRead())
                .forEach(n -> n.setIsRead(true));
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getMessage(), n.getChannel(),
                n.getIsRead(), n.getRelatedEntityType(), n.getRelatedEntityId(),
                n.getCreatedAt()
        );
    }

    private record OutboxEventPayload(
            UUID notificationId, UUID userId, String title, String message,
            String channel, String relatedEntityType, UUID relatedEntityId
    ) {}
}