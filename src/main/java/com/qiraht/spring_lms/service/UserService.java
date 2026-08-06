package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.annotation.Auditable;
import com.qiraht.spring_lms.dto.request.RegisterRequestDTO;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.ConflictException;
import com.qiraht.spring_lms.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Auditable(entityType = "user", action = "create", idExpr = "#result")
    public UUID RegisterUser(RegisterRequestDTO request) {
        // Check duplicate Email
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ConflictException("Email already in use");
        });

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
