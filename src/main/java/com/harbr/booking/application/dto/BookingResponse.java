package com.harbr.booking.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID propertyId,
        String propertyTitle,
        UUID guestId,
        String guestName,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        BigDecimal baseAmount,
        BigDecimal cleaningFee,
        BigDecimal serviceFee,
        BigDecimal totalPrice,
        String status,
        String cancellationReason,
        String cancelledBy,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {}