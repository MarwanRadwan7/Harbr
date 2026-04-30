package com.harbr.property.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdatePropertyRequest(
        String title,
        String description,
        PropertyTypeDto propertyType,
        Integer maxGuests,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal basePricePerNight,
        BigDecimal cleaningFee,
        Boolean isInstantBook,
        String status,
        AddressDto address,
        List<UUID> amenityIds
) {}