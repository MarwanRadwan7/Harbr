package com.harbr.property.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull PropertyTypeDto propertyType,
        @NotNull @Min(1) @Max(50) Integer maxGuests,
        @NotNull @Min(1) Integer bedrooms,
        @NotNull @Min(1) Integer bathrooms,
        @NotNull @DecimalMin("0.01") BigDecimal basePricePerNight,
        @DecimalMin("0") BigDecimal cleaningFee,
        Boolean isInstantBook,
        @NotNull AddressDto address,
        List<UUID> amenityIds
) {}