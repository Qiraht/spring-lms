package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.AssignmentRequestDTO;
import com.qiraht.spring_lms.dto.response.AssignmentResponseDTO;
import com.qiraht.spring_lms.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignment")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignmentService;

    @PostMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<String>> postAssignment(@PathVariable String classId, @RequestBody AssignmentRequestDTO request) {
        String data = assignmentService.addAssignment(classId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.OK.value(), "Assignment on class " + classId +" created successfully", data));
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponseDTO>> getAssignmentById(@PathVariable String assignmentId) {
        AssignmentResponseDTO data = assignmentService.getAssignmentById(assignmentId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<AssignmentResponseDTO>>> getAssignmentByClassId(@PathVariable String classId) {
        List<AssignmentResponseDTO> data = assignmentService.getAssignmentsByClass(classId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }
}
