package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.ClassRequestDTO;
import com.qiraht.spring_lms.dto.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassesService {
    private final ClassesRepository classesRepository;

    public void createClass(ClassRequestDTO request) {
        log.info("Creating class: {}", request.getName());

        // generate id with NanoID
        String classId = NanoIdUtils.randomNanoId(10);

        Classes _class = Classes.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        classesRepository.save(_class);
    }

    public List<ClassResponseDTO> getAllClasses() {
        List<ClassResponseDTO> response = new ArrayList<>();

        for (Classes _class : classesRepository.findAll()) {
            ClassResponseDTO responseDTO = new ClassResponseDTO();
            BeanUtils.copyProperties(_class, responseDTO);
            response.add(responseDTO);
        }

        return response;
    }

    public ClassResponseDTO getClassById(String id) {
        Classes _class = classesRepository.findById(id).orElseThrow(() -> new RuntimeException("Class with id " + id + " not found"));

        ClassResponseDTO responseDTO = new ClassResponseDTO();
        BeanUtils.copyProperties(_class, responseDTO);
        return responseDTO;
    }

    public void updateClass(String id,ClassRequestDTO request) {
        log.info("Putting class: {}", request.getName());
        Classes classes = classesRepository.findById(id).orElseThrow(() -> new RuntimeException("Class with id " + id + " not found"));

        classes.setName(request.getName());
        classes.setDescription(request.getDescription());

        classesRepository.save(classes);
    }

    public void deleteClass(String id) {
        log.info("Deleting class: {}", id);
        Classes classes = classesRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));

        classes.setDeletedAt(LocalDateTime.now());
    }


}
