package com.harbr.property.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PropertySearchRequest(
        String city,
        String country,
        String propertyType,
        Integer minGuests,
        Integer minBedrooms,
        Integer minBathrooms,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean isInstantBook,
        List<UUID> amenityIds,
        LocalDate checkIn,
        LocalDate checkOut,
        Double lat,
        Double lng,
        Double radiusKm,
        String query,
        int page,
        int size
) {
    public PropertySearchRequest {
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }
}