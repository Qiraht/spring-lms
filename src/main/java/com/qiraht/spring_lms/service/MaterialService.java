package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.MaterialRequestDTO;
import com.qiraht.spring_lms.dto.MaterialResponseDTO;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.MaterialRepository;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {
    private final ClassesRepository classesRepository;
    private final MaterialRepository materialRepository;

    public void addMaterial(MaterialRequestDTO request, String classId) {
        // Check class first
        classesRepository.findById(classId).orElseThrow(() -> new RuntimeException("Class not found"));

        String materialId = NanoIdUtils.randomNanoId(10);

        Material material = Material
                .builder()
                .id(materialId)
                .title(request.getTitle())
                .content(request.getContent())
                .attachment(request.getAttachment())
                .classId(classId)
                .build();

        materialRepository.save(material);
    }

    public MaterialResponseDTO getMaterialById(String materialId) {
        Material material = materialRepository.findById(materialId).orElseThrow(() -> new RuntimeException("Material not found"));

        MaterialResponseDTO response = new MaterialResponseDTO();

        BeanUtils.copyProperties(material, response);

        return response;
    }

    public List<MaterialResponseDTO> getAllMaterialsFromClass(String classId) {
        // Check class first
        classesRepository.findById(classId).orElseThrow(() -> new RuntimeException("Class not found"));

        List<Material> materials = materialRepository.findByClassId(classId);
        List<MaterialResponseDTO> response = new ArrayList<>();

        for (Material material : materials) {
            MaterialResponseDTO responseDTO = new MaterialResponseDTO();
            BeanUtils.copyProperties(material, responseDTO);
            response.add(responseDTO);
        }

        return response;
    }

    public void editMaterial(MaterialRequestDTO request, String materialId) {
        Material material = materialRepository.findById(materialId).orElseThrow(() -> new RuntimeException("Material not found"));

        material.setTitle(request.getTitle());
        material.setContent(request.getContent());
        material.setAttachment(request.getAttachment());

        materialRepository.save(material);
    }

    public void deleteMaterial(String materialId) {
        Material material = materialRepository.findById(materialId).orElseThrow(() -> new RuntimeException("Material not found"));

        material.setDeletedAt(LocalDateTime.now());

        materialRepository.save(material);
    }
}
