package com.harbr.review.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID propertyId,
        UUID bookingId,
        UUID guestId,
        String guestName,
        Integer rating,
        String comment,
        Instant createdAt
) {}