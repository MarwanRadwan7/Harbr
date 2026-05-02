package com.harbr.messaging.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Read userId set by JwtHandshakeInterceptor during HTTP handshake
            Object userIdAttr = accessor.getSessionAttributes().get("userId");
            if (userIdAttr == null) {
                throw new IllegalStateException("Missing userId in WebSocket session — handshake may have been rejected");
            }

            UUID userId = UUID.fromString(userIdAttr.toString());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of());
            accessor.setUser(authentication);
            log.debug("WebSocket authenticated: userId={}", userId);
        }

        return message;
    }
}