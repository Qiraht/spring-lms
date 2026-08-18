package com.qiraht.spring_lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qiraht.spring_lms.AbstractContainerTest;
import com.qiraht.spring_lms.Enum.UserRole;
import com.qiraht.spring_lms.dto.request.LoginRequestDTO;
import com.qiraht.spring_lms.dto.response.LoginResponseDTO;
import com.qiraht.spring_lms.dto.response.RefreshTokenResponseDTO;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.AuthenticationException;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class RefreshTokenRotationTest extends AbstractContainerTest {

    static final GenericContainer<?> valkey =
            new GenericContainer<>(DockerImageName.parse("valkey/valkey:7.2")).withExposedPorts(6379);

    static {
        valkey.start();
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", valkey::getHost);
        registry.add("spring.data.redis.port", () -> valkey.getMappedPort(6379));
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    void refreshRotatesTokenAndRejectsReuse() {
        // given: a registered user who logs in and obtains a refresh token
        User user = userRepository.save(User.builder()
                .email("refresh-test@example.com")
                .firstName("Refresh")
                .lastName("Test")
                .password(new BCryptPasswordEncoder(12).encode("Passw0rd!"))
                .role(UserRole.USER)
                .build());
        LoginResponseDTO login = authService.LoginUser(new LoginRequestDTO("refresh-test@example.com", "Passw0rd!"));
        String firstRefresh = login.getRefreshToken();

        // when: the refresh token is exchanged for a new token pair
        RefreshTokenResponseDTO rotated = authService.RefreshToken(firstRefresh);

        // then: a new refresh token is issued and the old one is revoked in Redis
        assertThat(rotated.getRefreshToken()).isNotEqualTo(firstRefresh);
        assertThat(refreshTokenService.isValid(jwtUtil.extractJti(firstRefresh)))
                .isFalse();

        // when: the now-revoked (replayed) refresh token is used again
        // then: it is rejected as revoked
        assertThatThrownBy(() -> authService.RefreshToken(firstRefresh)).isInstanceOf(AuthenticationException.class);
    }
}
