package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.dto.response.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.service.ClassesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
@Validated
public class ClassesController {
    private final ClassesService classesService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> postClass(@RequestBody ClassRequestDTO request) {
        classesService.createClass(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED.value(),"Class created successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassResponseDTO>>> getAllClasses() {
        List<ClassResponseDTO> data = classesService.getAllClasses();

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassResponseDTO>> getClassById(@PathVariable("id") String id) {
        ClassResponseDTO data = classesService.getClassById(id);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Classes>> putClass(@PathVariable("id") String id, @RequestBody ClassRequestDTO request) {
        classesService.updateClass(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable("id") String id) {
        classesService.deleteClass(id);

        return ResponseEntity.ok(ApiResponse.success(200,"success", null));
    }
}
