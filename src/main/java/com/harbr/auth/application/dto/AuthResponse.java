package com.harbr.auth.application.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserResponse user
) {
    public static final String TOKEN_TYPE = "Bearer";
}