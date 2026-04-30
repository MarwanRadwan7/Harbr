package com.harbr.property.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PropertyImageResponse(
        UUID id,
        String url,
        Integer displayOrder,
        Boolean isCover,
        Instant createdAt
) {}