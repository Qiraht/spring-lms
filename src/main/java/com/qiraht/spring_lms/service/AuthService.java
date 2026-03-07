package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.LoginRequestDTO;
import com.qiraht.spring_lms.dto.response.LoginResponseDTO;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO LoginUser(LoginRequestDTO request) {
        // Check for email
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new NotFoundException("Email not found"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email or Password Incorrect!");
        }

        // Check user active
        if (user.getDeletedAt() != null) {
            throw new BadCredentialsException("User is inactive!");
        }

        log.info("User {} logged in successfully", user.getEmail());

        final String token = jwtUtil.generateToken(user);

        return new LoginResponseDTO(request.getEmail(), token);
    }
}
