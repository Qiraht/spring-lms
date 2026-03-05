package com.qiraht.spring_lms.util;

import com.qiraht.spring_lms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSecretKey() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}

    public String extractEmail(String token) { return extractClaim(token, Claims::getSubject); }

    public UUID extractUserId(String token) {
        String id = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(id);
    }

    public String extractRole(String token) { return extractClaim(token, claims -> claims.get("role", String.class)); }

    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration);}

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name()); // role for Bypass
        claims.put("userId", user.getId().toString()); // id for resource authorization

        return createToken(claims, user.getEmail());
    };

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .issuer("spring-lms-api")
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSecretKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

}
