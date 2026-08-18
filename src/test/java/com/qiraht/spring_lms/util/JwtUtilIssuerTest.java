package com.qiraht.spring_lms.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiraht.spring_lms.Enum.UserRole;
import com.qiraht.spring_lms.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilIssuerTest {

    private static final String ACCESS_SECRET = "test-access-secret-needs-at-least-32-bytes-long-123456";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "accessSecret", ACCESS_SECRET);
        ReflectionTestUtils.setField(
                jwtUtil, "refreshSecret", "test-refresh-secret-needs-at-least-32-bytes-long-123456");
        ReflectionTestUtils.setField(jwtUtil, "accessExpiration", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", Duration.ofDays(1));
    }

    @Test
    void tokenWithExpectedIssuerPassesValidation() {
        // given: a token minted by JwtUtil (it sets issuer "spring-lms-api")
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("issuer-test@example.com")
                .role(UserRole.USER)
                .build();
        String token = jwtUtil.generateToken(user);

        // when: the token is validated
        // then: it is accepted
        assertThat(jwtUtil.validateAccessToken(token)).isTrue();
    }

    @Test
    void tokenWithoutIssuerFailsValidation() {
        // given: a token signed with the access secret but missing the required issuer
        SecretKey key = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("userId", UUID.randomUUID().toString());
        String token = Jwts.builder()
                .claims(claims)
                .subject("issuer-test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        // when: the token is validated
        // then: it is rejected because the issuer claim is absent
        assertThat(jwtUtil.validateAccessToken(token)).isFalse();
    }
}
