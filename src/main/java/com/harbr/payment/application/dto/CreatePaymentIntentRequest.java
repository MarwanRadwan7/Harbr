package com.harbr.payment.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentIntentRequest(
        @NotNull UUID bookingId,
        @NotNull @Positive BigDecimal amount,
        String currency
) {
    public CreatePaymentIntentRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}