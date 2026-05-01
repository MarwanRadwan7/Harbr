package com.harbr.payment.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.payment.application.PaymentService;
import com.harbr.payment.application.dto.CreatePaymentIntentRequest;
import com.harbr.payment.application.dto.PaymentIntentResponse;
import com.harbr.payment.infrastructure.PaymentGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    @PostMapping("/intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        log.info("Received payment webhook");

        PaymentGateway.WebhookResult result = paymentGateway.processWebhook(payload, sigHeader);
        if (result != null) {
            paymentService.processWebhookEvent(result);
        }

        return ResponseEntity.ok().build();
    }
}