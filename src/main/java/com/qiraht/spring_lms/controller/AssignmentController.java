package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.AssignmentRequestDTO;
import com.qiraht.spring_lms.dto.response.AssignmentResponseDTO;
import com.qiraht.spring_lms.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignment")
@RequiredArgsConstructor
@Validated
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping("/class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #classId)")
    public ResponseEntity<ApiResponse<String>> postAssignment(@PathVariable String classId,
            @RequestBody AssignmentRequestDTO request) {
        String data = assignmentService.addAssignment(classId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.OK.value(),
                "Assignment on class " + classId + " created successfully", data));
    }

    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isEnrolledInClass(authentication.principal.userId, @assignmentRepository.findById(#assignmentId).get().classes.id)")
    public ResponseEntity<ApiResponse<AssignmentResponseDTO>> getAssignmentById(@PathVariable String assignmentId) {
        AssignmentResponseDTO data = assignmentService.getAssignmentById(assignmentId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isEnrolledInClass(authentication.principal.userId, #classId)")
    public ResponseEntity<ApiResponse<List<AssignmentResponseDTO>>> getAssignmentByClassId(
            @PathVariable String classId) {
        List<AssignmentResponseDTO> data = assignmentService.getAssignmentsByClass(classId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isTeacherOfAssignment(authentication.principal.userId, #assignmentId)")
    public ResponseEntity<ApiResponse<String>> putAssignment(@PathVariable String assignmentId,
            @RequestBody AssignmentRequestDTO request) {
        String data = assignmentService.editAssignment(assignmentId, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN')") // Admin Only
    public ResponseEntity<ApiResponse<String>> deleteAssignment(@PathVariable String assignmentId) {
        String data = assignmentService.deleteAssignment(assignmentId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }
}
