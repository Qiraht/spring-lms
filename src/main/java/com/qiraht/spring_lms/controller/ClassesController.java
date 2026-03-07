package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.dto.response.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.service.ClassesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
@Validated
public class ClassesController {
    private final ClassesService classesService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Admin only can create class
    public ResponseEntity<ApiResponse<Void>> postClass(@RequestBody ClassRequestDTO request) {
        classesService.createClass(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Class created successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')") // Any Authenticated
    public ResponseEntity<ApiResponse<List<ClassResponseDTO>>> getAllClasses() {
        List<ClassResponseDTO> data = classesService.getAllClasses();

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ApiResponse<ClassResponseDTO>> getClassById(@PathVariable("id") String id) {
        ClassResponseDTO data = classesService.getClassById(id);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #id)")
    public ResponseEntity<ApiResponse<Classes>> putClass(@PathVariable("id") String id,
            @RequestBody ClassRequestDTO request) {
        classesService.updateClass(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Admin only
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable("id") String id) {
        classesService.deleteClass(id);

        return ResponseEntity.ok(ApiResponse.success(200, "success", null));
    }
}
