package com.harbr.payment.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "harbr.payment")
public class PaymentProperties {

    private String provider = "mock";
    private String stripeSecretKey;
    private String stripeWebhookSecret;
    private String stripeApiVersion = "2024-12-18.acacia";
}