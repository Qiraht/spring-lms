package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.LoginRequestDTO;
import com.qiraht.spring_lms.dto.LoginResponseDTO;
import com.qiraht.spring_lms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> postLogin(@RequestBody LoginRequestDTO request) {
        LoginResponseDTO data = authService.LoginUser(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder().status(HttpStatus.OK.value()).message("Login success").data(data).build());
    }
}
