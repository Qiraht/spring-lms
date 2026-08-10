package com.qiraht.spring_lms.util;

import com.qiraht.spring_lms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.access.secret}")
    private String accessSecret;

    @Value("${jwt.access.expiration}")
    private Duration accessExpiration;

    @Value("${jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${jwt.refresh.expiration}")
    private Duration refreshExpiration;

    private SecretKey getAccessSecretKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshSecretKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractUserId(String token) {
        String id = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(id);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return parseSignedClaims(token, getAccessSecretKey());
        } catch (Exception e) {
            return parseSignedClaims(token, getRefreshSecretKey());
        }
    }

    private Claims parseSignedClaims(String token, SecretKey key) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name()); // role for Bypass
        claims.put("userId", user.getId().toString()); // id for resource authorization
        claims.put("type", "access");

        return createToken(claims, user.getEmail(), accessExpiration, getAccessSecretKey());
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");

        return createToken(claims, user.getEmail(), refreshExpiration, getRefreshSecretKey());
    }

    private String createToken(Map<String, Object> claims, String subject, Duration ttl, SecretKey key) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .issuer("spring-lms-api")
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return isValid(token, getAccessSecretKey());
    }

    public boolean validateRefreshToken(String token) {
        return isValid(token, getRefreshSecretKey());
    }

    private boolean isValid(String token, SecretKey key) {
        try {
            Claims claims = parseSignedClaims(token, key);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
