package com.harbr.auth.application;

import com.harbr.auth.application.dto.*;
import com.harbr.auth.domain.PasswordResetToken;
import com.harbr.auth.domain.RefreshToken;
import com.harbr.auth.domain.Role;
import com.harbr.auth.domain.User;
import com.harbr.auth.infrastructure.PasswordResetTokenRepository;
import com.harbr.auth.infrastructure.RefreshTokenRepository;
import com.harbr.auth.infrastructure.UserRepository;
import com.harbr.auth.infrastructure.security.JwtProperties;
import com.harbr.auth.infrastructure.security.JwtService;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.ConflictException;
import com.harbr.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(Role.GUEST)
                .isVerified(false)
                .build();

        user = userRepository.save(user);
        return generateTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return generateTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String hashedToken = sha256(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        return generateTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "No account found with this email address"));

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = sha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashedToken)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset token generated for user: userId={}, email={}", user.getId(), user.getEmail());

        return rawToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hashedToken = sha256(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid or expired password reset token"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "Password reset token has expired");
        }

        if (resetToken.getIsUsed()) {
            throw new BusinessException("TOKEN_USED", "Password reset token has already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: userId={}", user.getId());
    }

    private AuthResponse generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedRefreshToken = sha256(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashedRefreshToken)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtProperties.getAccessTokenExpiration(),
                AuthResponse.TOKEN_TYPE,
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getIsVerified()
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}