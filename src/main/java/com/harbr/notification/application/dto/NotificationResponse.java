package com.harbr.notification.application.dto;

import com.harbr.notification.domain.NotificationChannel;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationChannel channel,
        Boolean isRead,
        String relatedEntityType,
        UUID relatedEntityId,
        Instant createdAt
) {}