package com.harbr.payment.application.dto;

public record PaymentIntentResponse(
        String clientSecret,
        String providerTxId,
        String provider
) {}