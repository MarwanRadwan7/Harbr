package com.harbr.auth.application;

import com.harbr.auth.application.dto.AdminUserResponse;
import com.harbr.auth.application.dto.UpdateRoleRequest;
import com.harbr.auth.domain.Role;
import com.harbr.auth.domain.User;
import com.harbr.auth.infrastructure.UserRepository;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toAdminUserResponse);
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        Role newRole;
        try {
            newRole = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_ROLE", "Role must be one of: GUEST, HOST, ADMIN");
        }

        user.setRole(newRole);
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse verifyUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        user.setIsVerified(true);
        user = userRepository.save(user);
        return toAdminUserResponse(user);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getIsVerified(),
                user.getCreatedAt()
        );
    }
}