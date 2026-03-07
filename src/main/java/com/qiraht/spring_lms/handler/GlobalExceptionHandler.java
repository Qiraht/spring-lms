package com.qiraht.spring_lms.handler;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.exception.AuthenticationException;
import com.qiraht.spring_lms.exception.AuthorizationException;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Manual Validation Error
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(ValidationException ex, HttpServletRequest request) {
        log.error("error while validation process", ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null));
    }

    // Not Found Error
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessError(NotFoundException ex, HttpServletRequest request) {
        log.error("error while processing request", ex);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage(), null));
    }

    // Authentication Error
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessError(AuthenticationException ex, HttpServletRequest request) {
        log.error("error while processing request", ex);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), null));
    }

    // Authorization Error
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessError(AuthorizationException ex, HttpServletRequest request) {
        log.error("error while processing request", ex);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage(), null));
    }

    // Jakarta Validation Error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("error while processing request", ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null));
    }

    // Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(Exception ex, HttpServletRequest request) {
        log.error("error while processing request", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), null));
    }
}
