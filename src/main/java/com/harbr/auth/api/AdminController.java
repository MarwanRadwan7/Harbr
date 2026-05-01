package com.harbr.auth.api;

import com.harbr.auth.application.AdminService;
import com.harbr.auth.application.dto.AdminUserResponse;
import com.harbr.auth.application.dto.UpdateRoleRequest;
import com.harbr.common.web.ApiResponse;
import com.harbr.common.web.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminUserResponse> users = adminService.listUsers(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(users)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        AdminUserResponse response = adminService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/users/{id}/verify")
    public ResponseEntity<ApiResponse<AdminUserResponse>> verifyUser(
            @PathVariable UUID id) {
        AdminUserResponse response = adminService.verifyUser(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}