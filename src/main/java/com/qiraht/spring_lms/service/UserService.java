package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.RegisterRequestDTO;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UUID RegisterUser(RegisterRequestDTO request) {
        // Check duplicate Email
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {throw new RuntimeException("Email already in use");});

        // Password hashing
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(hashedPassword)
                .build();

        userRepository.save(user);

        return user.getId();
    }
}
