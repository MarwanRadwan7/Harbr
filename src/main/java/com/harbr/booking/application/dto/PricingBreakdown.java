package com.harbr.booking.application.dto;

import java.math.BigDecimal;

public record PricingBreakdown(
        BigDecimal baseAmount,
        BigDecimal cleaningFee,
        BigDecimal serviceFee,
        BigDecimal totalPrice,
        int nights,
        BigDecimal nightlyRate
) {}