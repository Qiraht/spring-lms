package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.GradeRequestDTO;
import com.qiraht.spring_lms.dto.request.SubmissionRequestDTO;
import com.qiraht.spring_lms.dto.response.SubmissionResponseDTO;
import com.qiraht.spring_lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignment/{assignmentId}/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isEnrolledInClass(authentication.principal.userId, @assignmentRepository.findById(#assignmentId).get().classes.id)")
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> submitAssignment(
            @PathVariable String assignmentId,
            @RequestBody SubmissionRequestDTO request) {

        SubmissionResponseDTO data = submissionService.submitAssignment(assignmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Submission successful", data));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfAssignment(authentication.principal.userId, #assignmentId)")
    public ResponseEntity<ApiResponse<List<SubmissionResponseDTO>>> getAllSubmissions(
            @PathVariable String assignmentId) {
        List<SubmissionResponseDTO> data = submissionService.getAllSubmissions(assignmentId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/{submissionId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfAssignment(authentication.principal.userId, #assignmentId)")
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> getSubmission(
            @PathVariable String assignmentId,
            @PathVariable String submissionId) {

        SubmissionResponseDTO data = submissionService.getSubmissionById(submissionId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @PutMapping("/{submissionId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfAssignment(authentication.principal.userId, #assignmentId)")
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> gradeSubmission(
            @PathVariable String assignmentId,
            @PathVariable String submissionId,
            @RequestBody GradeRequestDTO request) {

        SubmissionResponseDTO data = submissionService.gradeSubmission(submissionId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Graded successfully", data));
    }
}
