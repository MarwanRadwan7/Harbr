package com.harbr.messaging.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.messaging.application.MessagingService;
import com.harbr.messaging.application.dto.MessageResponse;
import com.harbr.messaging.application.dto.SendMessageRequest;
import com.harbr.messaging.domain.Conversation;
import com.harbr.messaging.infrastructure.ConversationRepository;
import com.harbr.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketMessageHandler {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;

    @MessageMapping("/chat/{conversationId}")
    public void handleMessage(
            @DestinationVariable UUID conversationId,
            @Payload SendMessageRequest request,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        SendMessageRequest fixedRequest = new SendMessageRequest(conversationId, request.content());
        MessageResponse response = messagingService.sendMessage(userId, fixedRequest);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId, ApiResponse.ok(response));

        UUID recipientId = conversation.getGuest().getId().equals(userId)
                ? conversation.getHost().getId()
                : conversation.getGuest().getId();

        messagingTemplate.convertAndSendToUser(
                recipientId.toString(), "/queue/messages", ApiResponse.ok(response));
    }
}