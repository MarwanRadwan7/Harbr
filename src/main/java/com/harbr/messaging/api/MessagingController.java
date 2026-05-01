package com.harbr.messaging.api;

import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import com.harbr.messaging.application.MessagingService;
import com.harbr.messaging.application.dto.ConversationResponse;
import com.harbr.messaging.application.dto.CreateConversationRequest;
import com.harbr.messaging.application.dto.MessageResponse;
import com.harbr.messaging.application.dto.SendMessageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        ConversationResponse response = messagingService.createConversation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<ConversationResponse> conversations = messagingService.listConversations(userId);
        return ResponseEntity.ok(ApiResponse.ok(conversations));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        SendMessageRequest fixedRequest = new SendMessageRequest(conversationId, request.content());
        MessageResponse response = messagingService.sendMessage(userId, fixedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> listMessages(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        Page<MessageResponse> messages = messagingService.listMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(messages)));
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable UUID conversationId) {
        UUID userId = (UUID) authentication.getPrincipal();
        messagingService.markAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}