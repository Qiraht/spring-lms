package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.LoginRequestDTO;
import com.qiraht.spring_lms.dto.RegisterRequestDTO;
import com.qiraht.spring_lms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UUID>> postRegister(@RequestBody RegisterRequestDTO request) {
        UUID data = userService.RegisterUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,"success", data));
    }

    @PostMapping("/login")
    public void postLogin(@RequestBody LoginRequestDTO request) {}
}
