package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.annotation.Auditable;
import com.qiraht.spring_lms.dto.request.MaterialRequestDTO;
import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.dto.response.MaterialResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.MaterialRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {
    private final ClassesRepository classesRepository;
    private final MaterialRepository materialRepository;
    private final com.qiraht.spring_lms.repository.UserRepository userRepository;

    @Auditable(entityType = "material", action = "create", idExpr = "#result")
    public UUID addMaterial(MaterialRequestDTO request, String classId) {
        // Check class first
        Classes classes = classesRepository
                .findById(UUID.fromString(classId))
                .orElseThrow(() -> new NotFoundException("Class not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Material material = Material.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .attachment(request.getAttachment())
                .classes(classes)
                .user(user)
                .build();

        return materialRepository.save(material).getId();
    }

    public MaterialResponseDTO getMaterialById(String materialId) {
        Material material = materialRepository
                .findById(UUID.fromString(materialId))
                .orElseThrow(() -> new NotFoundException("Material not found"));

        MaterialResponseDTO response = new MaterialResponseDTO();

        BeanUtils.copyProperties(material, response);
        response.setId(material.getId().toString());
        if (material.getUser() != null) {
            response.setAuthor(AuthorDTO.builder()
                    .id(material.getUser().getId())
                    .firstName(material.getUser().getFirstName())
                    .lastName(material.getUser().getLastName())
                    .build());
        }

        return response;
    }

    public Page<MaterialResponseDTO> getAllMaterialsFromClass(String classId, Pageable pageable) {
        // Check class first
        classesRepository
                .findById(UUID.fromString(classId))
                .orElseThrow(() -> new NotFoundException("Class not found"));

        Page<Material> materials = materialRepository.findByClassesId(UUID.fromString(classId), pageable);

        return materials.map(material -> {
            MaterialResponseDTO responseDTO = new MaterialResponseDTO();
            BeanUtils.copyProperties(material, responseDTO);
            responseDTO.setId(material.getId().toString());
            if (material.getUser() != null) {
                responseDTO.setAuthor(AuthorDTO.builder()
                        .id(material.getUser().getId())
                        .firstName(material.getUser().getFirstName())
                        .lastName(material.getUser().getLastName())
                        .build());
            }
            return responseDTO;
        });
    }

    @Auditable(entityType = "material", action = "update", idExpr = "#materialId")
    public void editMaterial(MaterialRequestDTO request, String materialId) {
        Material material = materialRepository
                .findById(UUID.fromString(materialId))
                .orElseThrow(() -> new NotFoundException("Material not found"));

        material.setTitle(request.getTitle());
        material.setContent(request.getContent());
        material.setAttachment(request.getAttachment());

        materialRepository.save(material);
    }

    @Auditable(entityType = "material", action = "delete", idExpr = "#materialId")
    public void deleteMaterial(String materialId) {
        Material material = materialRepository
                .findById(UUID.fromString(materialId))
                .orElseThrow(() -> new NotFoundException("Material not found"));

        material.setDeletedAt(LocalDateTime.now());

        materialRepository.save(material);
    }
}
