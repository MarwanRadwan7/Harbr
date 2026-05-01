package com.harbr.notification.application;

import com.harbr.notification.domain.NotificationOutbox;
import com.harbr.notification.domain.OutboxStatus;
import com.harbr.notification.infrastructure.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String NOTIFICATION_EXCHANGE = "harbr.notifications";

    private final NotificationOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<NotificationOutbox> pending = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (NotificationOutbox event : pending) {
            try {
                String routingKey = event.getAggregateType() + "." + event.getEventType();
                rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, routingKey, event.getPayload());

                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);

                log.info("Published outbox event: id={}, type={}, routingKey={}",
                        event.getId(), event.getEventType(), routingKey);
            } catch (Exception e) {
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
            }
        }
    }
}