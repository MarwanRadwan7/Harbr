package com.harbr.notification.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    @RabbitListener(queues = "harbr.notification.events")
    public void handleNotificationEvent(String payload) {
        log.info("Received notification event: {}", payload);

        try {
            processEvent(payload);
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    private void processEvent(String payload) {
        log.info("Processing notification event for delivery (email/push simulation): {}", payload);

        // In production, this would dispatch to:
        // - Email service (e.g., SendGrid, SES)
        // - Push notification service (e.g., Firebase Cloud Messaging, APNs)
        // - SMS service (e.g., Twilio)
        // For now, we log the event as a simulation of delivery
    }
}