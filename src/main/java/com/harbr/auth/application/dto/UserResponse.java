package com.harbr.auth.application.dto;

import com.harbr.auth.domain.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Role role,
        String avatarUrl,
        Boolean isVerified
) {}