package com.harbr.auth.api;

import com.harbr.auth.application.UserService;
import com.harbr.auth.application.dto.ChangePasswordRequest;
import com.harbr.auth.application.dto.UpdateProfileRequest;
import com.harbr.auth.application.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<com.harbr.common.web.ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(com.harbr.common.web.ApiResponse.ok(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<com.harbr.common.web.ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(com.harbr.common.web.ApiResponse.ok(response));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<com.harbr.common.web.ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(com.harbr.common.web.ApiResponse.ok());
    }
}