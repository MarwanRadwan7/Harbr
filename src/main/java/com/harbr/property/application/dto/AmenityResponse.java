package com.harbr.property.application.dto;

import java.util.UUID;

public record AmenityResponse(
        UUID id,
        String name,
        String category,
        String iconKey
) {}