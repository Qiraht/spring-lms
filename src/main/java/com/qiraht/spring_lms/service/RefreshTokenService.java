package com.qiraht.spring_lms.service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_KEY = "refresh:token:";
    private static final String USER_REFRESH_KEY = "refresh:user:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh.expiration}")
    private Duration refreshExpiration;

    public void save(String jti, UUID userId) {
        redisTemplate.opsForValue().set(REFRESH_KEY + jti, userId.toString(), refreshExpiration);
        redisTemplate.opsForSet().add(USER_REFRESH_KEY + userId, jti);
    }

    public boolean isValid(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_KEY + jti));
    }

    public void revokeAllForUser(UUID userId) {
        Set<String> jtis = redisTemplate.opsForSet().members(USER_REFRESH_KEY + userId);
        if (jtis != null) {
            jtis.forEach(jti -> redisTemplate.delete(REFRESH_KEY + jti));
        }
        redisTemplate.delete(USER_REFRESH_KEY + userId);
    }
}
