package com.harbr.property.application.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateAvailabilityRuleRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String ruleType,
        java.math.BigDecimal priceOverride,
        Integer minStayNights,
        String note
) {}