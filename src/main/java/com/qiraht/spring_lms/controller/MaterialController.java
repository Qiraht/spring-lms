package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.MaterialRequestDTO;
import com.qiraht.spring_lms.dto.MaterialResponseDTO;
import com.qiraht.spring_lms.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
@Validated
public class MaterialController {
    private final MaterialService materialService;

    @PostMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<?>> postMaterial(@PathVariable String classId, @RequestBody MaterialRequestDTO request){
        materialService.addMaterial(request,classId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,"success", null));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiResponse<List<MaterialResponseDTO>>> getMaterials(@PathVariable String classId){
        List<MaterialResponseDTO> data = materialService.getAllMaterialsFromClass(classId);

        // TODO: Include class detail
        return ResponseEntity.ok(ApiResponse.success(200,"success", data));
    }

    @GetMapping("/{materialId}")
    public ResponseEntity<ApiResponse<MaterialResponseDTO>> getMaterial(@PathVariable String materialId){
        MaterialResponseDTO data = materialService.getMaterialById(materialId);

        return ResponseEntity.ok(ApiResponse.success(200,"success", data));
    }

    @PutMapping("/{materialId}")
    public ResponseEntity<ApiResponse<?>> putMaterial(@PathVariable String materialId, @RequestBody MaterialRequestDTO request){
        materialService.editMaterial(request,materialId);


        return ResponseEntity.ok(ApiResponse.success(200,"success", null));
    }

    @DeleteMapping("/{materialId}")
    public ResponseEntity<ApiResponse<?>> deleteMaterial(@PathVariable String materialId){
        materialService.deleteMaterial(materialId);

        return ResponseEntity.ok(ApiResponse.success(200,"success", null));
    }
}
