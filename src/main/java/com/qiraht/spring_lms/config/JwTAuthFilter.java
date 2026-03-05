package com.qiraht.spring_lms.config;

import com.qiraht.spring_lms.security.CustomUsersDetails;
import com.qiraht.spring_lms.service.UserDetailsServiceImpl;
import com.qiraht.spring_lms.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwTAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (token != null && jwtUtil.validateToken(token)) {
                authenticateUser(token, request);
            }
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }


        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        // Extract claims dari token
        String email = jwtUtil.extractEmail(token);
        UUID userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // Build CustomUsersDetails dari claims (no DB query!)
        CustomUsersDetails userDetails = CustomUsersDetails.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .build();

        // Create authentication token
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("Authenticated user: {} (ID: {}, Role: {})", email, userId, role);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
    }


}
