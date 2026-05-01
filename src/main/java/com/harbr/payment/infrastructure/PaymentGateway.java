package com.harbr.payment.infrastructure;

import java.math.BigDecimal;

public interface PaymentGateway {

    CreatePaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String metadataBookingId);

    WebhookResult processWebhook(String payload, String sigHeader);

    record CreatePaymentIntentResult(String clientSecret, String providerTxId) {}

    record WebhookResult(String providerTxId, String eventType, BigDecimal amount, String currency, boolean success, String failureReason) {}
}