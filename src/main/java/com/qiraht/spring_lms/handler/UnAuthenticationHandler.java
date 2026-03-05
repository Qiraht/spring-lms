package com.qiraht.spring_lms.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiraht.spring_lms.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UnAuthenticationHandler implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<?> body = ApiResponse.error(HttpStatus.FORBIDDEN.value(), authException.getMessage(), null);

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}
