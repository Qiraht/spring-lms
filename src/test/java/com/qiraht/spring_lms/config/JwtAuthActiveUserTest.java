package com.qiraht.spring_lms.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qiraht.spring_lms.AbstractContainerTest;
import com.qiraht.spring_lms.Enum.UserRole;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.util.JwtUtil;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

class JwtAuthActiveUserTest extends AbstractContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Test
    void activeUserCanAuthenticateAndDeletedUserIsRejected() throws Exception {
        // given: an active user and a valid access token minted for that user
        User user = User.builder()
                .email("active-user-test@example.com")
                .firstName("Active")
                .lastName("User")
                .password(new BCryptPasswordEncoder(12).encode("Passw0rd!"))
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        // when: a protected endpoint is called with the valid token
        // then: the request is authenticated and returns 200
        mockMvc.perform(get("/api/class")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // given: the same user is soft-deleted (deletedAt set) and a fresh token is minted
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        String revokedToken = jwtUtil.generateToken(user);

        // when: the protected endpoint is called with the token of the now-inactive user
        // then: the filter rejects the inactive user and the request is unauthenticated (401)
        mockMvc.perform(get("/api/class")
                        .header("Authorization", "Bearer " + revokedToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
