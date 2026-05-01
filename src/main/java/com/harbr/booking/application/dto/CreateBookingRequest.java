package com.harbr.booking.application.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID propertyId,
        @NotNull @Future LocalDate checkIn,
        @NotNull @Future LocalDate checkOut,
        @Min(1) @Max(50) int guestCount
) {}