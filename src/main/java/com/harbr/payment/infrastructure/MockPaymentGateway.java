package com.harbr.payment.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "harbr.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public CreatePaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String metadataBookingId) {
        String mockTxId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String mockClientSecret = mockTxId + "_secret_mock";
        log.info("Mock PaymentIntent created: providerTxId={}, amount={}", mockTxId, amount);
        return new CreatePaymentIntentResult(mockClientSecret, mockTxId);
    }

    @Override
    public WebhookResult processWebhook(String payload, String sigHeader) {
        log.warn("MockPaymentGateway does not process real webhooks. Use StripePaymentGateway for production.");
        return null;
    }
}