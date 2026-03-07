package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.MaterialRequestDTO;
import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.dto.response.MaterialResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.MaterialRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final com.qiraht.spring_lms.repository.UserRepository userRepository;

    public void addMaterial(MaterialRequestDTO request, String classId) {
        // Check class first
        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String materialId = NanoIdUtils.randomNanoId(10);

        Material material = Material
                .builder()
                .id(materialId)
                .title(request.getTitle())
                .content(request.getContent())
                .attachment(request.getAttachment())
                .classes(classes)
                .user(user)
                .build();

        materialRepository.save(material);
    }

    public MaterialResponseDTO getMaterialById(String materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found"));

        MaterialResponseDTO response = new MaterialResponseDTO();

        BeanUtils.copyProperties(material, response);
        if (material.getUser() != null) {
            response.setAuthor(AuthorDTO.builder()
                    .id(material.getUser().getId())
                    .firstName(material.getUser().getFirstName())
                    .lastName(material.getUser().getLastName())
                    .build());
        }

        return response;
    }

    public List<MaterialResponseDTO> getAllMaterialsFromClass(String classId) {
        // Check class first
        classesRepository.findById(classId).orElseThrow(() -> new RuntimeException("Class not found"));

        List<Material> materials = materialRepository.findByClassesId(classId);
        List<MaterialResponseDTO> response = new ArrayList<>();

        for (Material material : materials) {
            MaterialResponseDTO responseDTO = new MaterialResponseDTO();
            BeanUtils.copyProperties(material, responseDTO);
            if (material.getUser() != null) {
                responseDTO.setAuthor(com.qiraht.spring_lms.dto.response.AuthorDTO.builder()
                        .id(material.getUser().getId())
                        .firstName(material.getUser().getFirstName())
                        .lastName(material.getUser().getLastName())
                        .build());
            }
            response.add(responseDTO);
        }

        return response;
    }

    public void editMaterial(MaterialRequestDTO request, String materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found"));

        material.setTitle(request.getTitle());
        material.setContent(request.getContent());
        material.setAttachment(request.getAttachment());

        materialRepository.save(material);
    }

    public void deleteMaterial(String materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found"));

        material.setDeletedAt(LocalDateTime.now());

        materialRepository.save(material);
    }
}
