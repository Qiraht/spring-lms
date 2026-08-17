package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.LoginRequestDTO;
import com.qiraht.spring_lms.dto.request.RefreshTokenRequestDTO;
import com.qiraht.spring_lms.dto.response.LoginResponseDTO;
import com.qiraht.spring_lms.dto.response.RefreshTokenResponseDTO;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import com.qiraht.spring_lms.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    @Tag(name = "Auth")
    @Operation(
            summary = "Post Auth User",
            description = "Authenticate user and return JWT access + refresh token. Public API")
    @RateLimiter(name = "default", fallbackMethod = "loginFallback")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> postLogin(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO data = authService.LoginUser(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message("Login success")
                        .data(data)
                        .build());
    }

    private ResponseEntity<ApiResponse<Object>> loginFallback(LoginRequestDTO request, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message("Too many requests, please try again later")
                        .data(null)
                        .build());
    }

    @Tag(name = "Auth")
    @Operation(
            summary = "Refresh Token",
            description = "Exchange a valid refresh token for a new access token. Public API")
    @RateLimiter(name = "default", fallbackMethod = "refreshFallback")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> postRefresh(@RequestBody RefreshTokenRequestDTO request) {
        RefreshTokenResponseDTO data = authService.RefreshToken(request.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message("Token refreshed")
                        .data(data)
                        .build());
    }

    private ResponseEntity<ApiResponse<Object>> refreshFallback(RefreshTokenRequestDTO request, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .message("Too many requests, please try again later")
                        .data(null)
                        .build());
    }

    @Tag(name = "Auth")
    @Operation(
            summary = "Logout",
            description = "Revoke all refresh tokens for the authenticated user. Authentication needed")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> postLogout(Authentication authentication) {
        CustomUsersDetails principal = (CustomUsersDetails) authentication.getPrincipal();
        UUID userId = principal.getUserId();
        authService.Logout(userId);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Logout success", null));
    }
}
