package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.MaterialRequestDTO;
import com.qiraht.spring_lms.dto.response.MaterialResponseDTO;
import com.qiraht.spring_lms.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
@Validated
public class MaterialController {
    private final MaterialService materialService;

    @PostMapping("/class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfClass(authentication.principal.userId, #classId)")
    public ResponseEntity<ApiResponse<?>> postMaterial(@PathVariable String classId,
            @RequestBody MaterialRequestDTO request) {
        materialService.addMaterial(request, classId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201, "success", null));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isEnrolledInClass(authentication.principal.userId, #classId)")
    public ResponseEntity<ApiResponse<List<MaterialResponseDTO>>> getMaterials(@PathVariable String classId) {
        List<MaterialResponseDTO> data = materialService.getAllMaterialsFromClass(classId);

        // TODO: Include class detail
        return ResponseEntity.ok(ApiResponse.success(200, "success", data));
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isEnrolledInClass(authentication.principal.userId, @materialRepository.findById(#materialId).get().classes.id)")
    public ResponseEntity<ApiResponse<MaterialResponseDTO>> getMaterial(@PathVariable String materialId) {
        MaterialResponseDTO data = materialService.getMaterialById(materialId);

        return ResponseEntity.ok(ApiResponse.success(200, "success", data));
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN') or @enrollmentService.isTeacherOfMaterial(authentication.principal.userId, #materialId)")
    public ResponseEntity<ApiResponse<?>> putMaterial(@PathVariable String materialId,
            @RequestBody MaterialRequestDTO request) {
        materialService.editMaterial(request, materialId);

        return ResponseEntity.ok(ApiResponse.success(200, "success", null));
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasRole('ADMIN')") // Admin Only
    public ResponseEntity<ApiResponse<?>> deleteMaterial(@PathVariable String materialId) {
        materialService.deleteMaterial(materialId);

        return ResponseEntity.ok(ApiResponse.success(200, "success", null));
    }
}
