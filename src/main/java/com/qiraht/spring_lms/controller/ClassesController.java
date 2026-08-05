package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.dto.response.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.service.ClassesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
@Validated
public class ClassesController {
    private final ClassesService classesService;

    @Tag(name = "Class")
    @Operation(
            summary = "Create Class",
            description = "Create new Class. Authentication Needed and role 'Admin' needed ")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Admin only can create class
    public ResponseEntity<ApiResponse<Void>> postClass(@RequestBody ClassRequestDTO request) {
        classesService.createClass(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Class created successfully", null));
    }

    @Tag(name = "Class")
    @Operation(summary = "Get All Classes", description = "Return All Classes. Authentication Needed")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')") // Any Authenticated
    public ResponseEntity<ApiResponse<Page<ClassResponseDTO>>> getAllClasses(Pageable pageable) {
        Page<ClassResponseDTO> data = classesService.getAllClasses(pageable);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @Tag(name = "class")
    @Operation(summary = "Get Class By Id", description = "Return a class by id. Authentication Needed")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ApiResponse<ClassResponseDTO>> getClassById(@PathVariable("id") String id) {
        ClassResponseDTO data = classesService.getClassById(id);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @Tag(name = "Class")
    @Operation(
            summary = "Put Class",
            description =
                    "Edit Class details. Authentication Needed and Resource Authorization role 'TEACHER' (User role 'ADMIN' can bypass this)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #id)")
    public ResponseEntity<ApiResponse<Classes>> putClass(
            @PathVariable("id") String id, @RequestBody ClassRequestDTO request) {
        classesService.updateClass(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", null));
    }

    @Tag(name = "Class")
    @Operation(
            summary = "Delete Class",
            description = "Delete Class (Soft Delete). Authentication Needed and role 'ADMIN' needed")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Admin only
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable("id") String id) {
        classesService.deleteClass(id);

        return ResponseEntity.ok(ApiResponse.success(200, "success", null));
    }
}
