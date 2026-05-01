package com.harbr.messaging.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateConversationRequest(
        @NotNull UUID propertyId,
        @NotBlank String initialMessage
) {}