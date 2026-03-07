package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.EnrollRequestDTO;
import com.qiraht.spring_lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/class/{classId}/enroll")
@RequiredArgsConstructor
@Validated
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enrollUsers(
            @PathVariable String classId,
            @RequestBody List<EnrollRequestDTO> requests) {

        enrollmentService.enrollUsers(classId, requests);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Users enrolled successfully", null));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeUser(
            @PathVariable String classId,
            @PathVariable UUID userId) {

        enrollmentService.removeUserFromClass(classId, userId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "User removed from class successfully", null));
    }
}
