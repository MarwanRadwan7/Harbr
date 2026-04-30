package com.harbr.property.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String title,
        String description,
        String propertyType,
        Integer maxGuests,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal basePricePerNight,
        BigDecimal cleaningFee,
        String status,
        Boolean isInstantBook,
        Instant createdAt,
        Instant updatedAt,
        UUID hostId,
        String hostName,
        AddressDto address,
        List<PropertyImageResponse> images,
        List<AmenityResponse> amenities
) {}