package com.harbr.messaging.application.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        Boolean isRead,
        Instant createdAt
) {}