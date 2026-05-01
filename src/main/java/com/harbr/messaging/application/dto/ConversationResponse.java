package com.harbr.messaging.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID propertyId,
        String propertyTitle,
        UUID guestId,
        String guestName,
        UUID hostId,
        String hostName,
        String lastMessageContent,
        Instant lastMessageAt,
        Long unreadCount,
        Instant createdAt
) {}