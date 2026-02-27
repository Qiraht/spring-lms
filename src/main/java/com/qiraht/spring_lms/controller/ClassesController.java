package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.ClassRequestDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.service.ClassesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassesController {
    private final ClassesService classesService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> postClass(@RequestBody ClassRequestDTO request) {
        classesService.createClass(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201,"Class created successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Classes>>> getAllClasses() {
        List<Classes> classes = classesService.getAllClasses();

        return ResponseEntity.ok(ApiResponse.success(200, "success", classes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Classes>> getClassById(@PathVariable("id") String id) {
        Classes classes = classesService.getClassById(id);

        return ResponseEntity.ok(ApiResponse.success(200,"success", classes));
    }
}
