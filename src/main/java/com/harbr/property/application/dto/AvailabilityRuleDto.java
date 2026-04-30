package com.harbr.property.application.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilityRuleDto(
        UUID id,
        LocalDate startDate,
        LocalDate endDate,
        String ruleType,
        java.math.BigDecimal priceOverride,
        Integer minStayNights,
        String note
) {}