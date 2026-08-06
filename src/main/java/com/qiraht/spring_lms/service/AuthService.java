package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.LoginRequestDTO;
import com.qiraht.spring_lms.dto.response.LoginResponseDTO;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.AuthenticationException;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.util.JwtUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    public LoginResponseDTO LoginUser(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        try {
            if (user == null) {
                throw new AuthenticationException("Email or Password Incorrect!");
            }

            // Check password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new AuthenticationException("Email or Password Incorrect!");
            }

            // Check user active
            if (user.getDeletedAt() != null) {
                throw new AuthenticationException("User is inactive!");
            }

            log.info("User {} logged in successfully", user.getEmail());

            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);
            refreshTokenService.save(jwtUtil.extractJti(refreshToken), user.getId());

            auditService.record(user.getId(), "user", user.getId(), "login", "success", null, null);

            return new LoginResponseDTO(user.getEmail(), accessToken, refreshToken, accessTokenExpiration);
        } catch (Exception ex) {
            if (user != null) {
                auditService.record(user.getId(), "user", user.getId(), "login", "failed", null, null);
            }
            throw ex;
        }
    }

    public LoginResponseDTO RefreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String jti = jwtUtil.extractJti(refreshToken);
        UUID userId = jwtUtil.extractUserId(refreshToken);

        if (!refreshTokenService.isValid(jti)) {
            throw new AuthenticationException("Refresh token has been revoked");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getDeletedAt() != null) {
            throw new AuthenticationException("User is inactive!");
        }

        // Rotate: revoke old refresh token, issue a new one
        refreshTokenService.revoke(jti, userId);

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        refreshTokenService.save(jwtUtil.extractJti(newRefreshToken), userId);

        auditService.record(userId, "user", userId, "refresh", "success", null, null);

        log.info("User {} refreshed token", user.getEmail());

        return new LoginResponseDTO(user.getEmail(), newAccessToken, newRefreshToken, accessTokenExpiration);
    }

    public void Logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        auditService.record(userId, "user", userId, "logout", "success", null, null);
        log.info("User {} logged out", userId);
    }
}
