package com.harbr.payment.infrastructure;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "harbr.payment.provider", havingValue = "stripe")
public class StripePaymentGateway implements PaymentGateway {

    private final PaymentProperties paymentProperties;

    @Override
    public CreatePaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String metadataBookingId) {
        try {
            com.stripe.Stripe.apiKey = paymentProperties.getStripeSecretKey();

            var params = new java.util.HashMap<String, Object>();
            params.put("amount", amount.movePointRight(2).longValue());
            params.put("currency", currency.toLowerCase());
            params.put("automatic_payment_methods", java.util.Map.of("enabled", true));
            params.put("metadata", java.util.Map.of("bookingId", metadataBookingId));

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("Created Stripe PaymentIntent: id={}, amount={}", paymentIntent.getId(), amount);
            return new CreatePaymentIntentResult(paymentIntent.getClientSecret(), paymentIntent.getId());
        } catch (Exception e) {
            log.error("Failed to create Stripe PaymentIntent", e);
            throw new RuntimeException("Payment intent creation failed", e);
        }
    }

    @Override
    public WebhookResult processWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, paymentProperties.getStripeWebhookSecret());

            if ("payment_intent.succeeded".equals(event.getType())) {
                return handlePaymentSuccess(event);
            } else if ("payment_intent.payment_failed".equals(event.getType())) {
                return handlePaymentFailure(event);
            }

            log.info("Unhandled Stripe event type: {}", event.getType());
            return null;
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed", e);
            throw new RuntimeException("Invalid webhook signature", e);
        }
    }

    private WebhookResult handlePaymentSuccess(Event event) {
        PaymentIntent pi = deserializePaymentIntent(event);
        if (pi == null) return null;

        return new WebhookResult(
                pi.getId(),
                event.getType(),
                new BigDecimal(pi.getAmount()).movePointLeft(2),
                pi.getCurrency(),
                true,
                null
        );
    }

    private WebhookResult handlePaymentFailure(Event event) {
        PaymentIntent pi = deserializePaymentIntent(event);
        if (pi == null) return null;

        String failureReason = pi.getLastPaymentError() != null
                ? pi.getLastPaymentError().getMessage()
                : "Payment failed";

        return new WebhookResult(
                pi.getId(),
                event.getType(),
                new BigDecimal(pi.getAmount()).movePointLeft(2),
                pi.getCurrency(),
                false,
                failureReason
        );
    }

    private PaymentIntent deserializePaymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (obj instanceof PaymentIntent) {
                return (PaymentIntent) obj;
            }
        }
        log.warn("Could not deserialize Stripe event data: type={}", event.getType());
        return null;
    }
}