package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.RegisterRequestDTO;
import com.qiraht.spring_lms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @Tag(name = "User")
    @Operation(summary = "Post Register User", description = "Register new User with default role 'USER'. Public API")
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> postRegister(@RequestBody RegisterRequestDTO request) {
        UUID data = userService.RegisterUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201, "success", data));
    }
}
