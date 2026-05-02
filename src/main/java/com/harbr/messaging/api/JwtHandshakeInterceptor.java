package com.harbr.messaging.api;

import com.harbr.auth.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        log.debug("WebSocket handshake attempt from: {}", request.getRemoteAddress());
        
        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing Authorization header. Headers: {}", request.getHeaders());
            return false;
        }
        
        log.debug("Token extracted, length: {}", token.length());

        try {
            boolean valid = jwtService.validateToken(token);
            log.debug("Token validation result: {}", valid);
            
            if (!valid) {
                log.warn("WebSocket handshake rejected: token validation failed");
                return false;
            }
            
            UUID userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token).name();

            attributes.put("userId", userId.toString());
            attributes.put("email", email);
            attributes.put("role", role);

            log.debug("WebSocket handshake SUCCESS: userId={} email={}", userId, email);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: token invalid or expired - {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        // Try HTTP Authorization header (case-insensitive lookup)
        var headers = request.getHeaders();
        String auth = null;
        
        // HttpHeaders is case-insensitive by default, so just try common variations
        auth = headers.getFirst("Authorization");
        if (auth == null) {
            auth = headers.getFirst("authorization");
        }
        
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7).trim();
        }

        // Try query parameter
        if (request instanceof ServletServerHttpRequest sReq) {
            HttpServletRequest httpReq = sReq.getServletRequest();
            String tokenParam = httpReq.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                return tokenParam.trim();
            }

            // Try cookie
            jakarta.servlet.http.Cookie[] cookies = httpReq.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("token".equals(cookie.getName()) || "accessToken".equals(cookie.getName())) {
                        String cookieValue = cookie.getValue();
                        if (cookieValue != null && !cookieValue.isBlank()) {
                            return cookieValue.trim();
                        }
                    }
                }
            }
        }

        return null;
    }
}