package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.response.StudentClassSummaryDTO;
import com.qiraht.spring_lms.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/class/{classId}")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #classId)")
    public ResponseEntity<ApiResponse<Page<StudentClassSummaryDTO>>> getAllStudentSummaries(
            @PathVariable String classId, Pageable pageable) {

        Page<StudentClassSummaryDTO> data = progressService.getAllStudentSummariesForClass(classId, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/summary/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #classId) or authentication.principal.userId == #studentId")
    public ResponseEntity<ApiResponse<StudentClassSummaryDTO>> getStudentSummary(
            @PathVariable String classId,
            @PathVariable UUID studentId) {

        StudentClassSummaryDTO data = progressService.getStudentClassSummary(classId, studentId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }
}
