package com.qiraht.spring_lms.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiraht.spring_lms.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle
            ( HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<?> body = ApiResponse.error(HttpStatus.FORBIDDEN.value(), accessDeniedException.getMessage(), null);

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}
