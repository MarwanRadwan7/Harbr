package com.harbr.messaging.api;

import com.harbr.messaging.application.MessagingService;
import com.harbr.messaging.application.dto.SendMessageRequest;
import com.harbr.messaging.domain.Conversation;
import com.harbr.messaging.infrastructure.ConversationRepository;
import com.harbr.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageHandler {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;

    @MessageMapping("/chat/{conversationId}")
    public void handleMessage(
            @DestinationVariable UUID conversationId,
            @Payload SendMessageRequest request,
            Authentication authentication) {

        log.debug("Received WebSocket message for conversation: {}", conversationId);

        if (authentication == null || authentication.getPrincipal() == null) {
            log.error("Authentication is null - user not authenticated via WebSocket");
            throw new IllegalStateException("User not authenticated");
        }

        UUID userId = (UUID) authentication.getPrincipal();
        log.debug("User {} sending message to conversation {}", userId, conversationId);

        try {
            // Service now handles DB save AND WebSocket broadcast
            messagingService.sendMessage(userId, new SendMessageRequest(conversationId, request.content()));

            log.debug("Message sent successfully via service");
        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage(), e);
            throw e;
        }
    }
}