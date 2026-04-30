package com.harbr.property.application.dto;

import jakarta.validation.constraints.*;

public record AddressDto(
        @NotBlank @Size(max = 300) String street,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String state,
        @NotBlank @Size(max = 100) String country,
        @Size(max = 20) String postalCode,
        @NotNull Double lat,
        @NotNull Double lng
) {}