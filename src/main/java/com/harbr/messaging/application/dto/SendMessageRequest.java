package com.harbr.messaging.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        @NotBlank String content
) {}