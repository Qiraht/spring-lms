package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.dto.response.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
                .id(classId)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        classesRepository.save(_class);
    }

    public Page<ClassResponseDTO> getAllClasses(Pageable pageable) {
        return classesRepository.findAll(pageable).map(classes -> {
            ClassResponseDTO responseDTO = new ClassResponseDTO();
            BeanUtils.copyProperties(classes, responseDTO);
            return responseDTO;
        });
    }

    public ClassResponseDTO getClassById(String id) {
        Classes _class = classesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        ClassResponseDTO responseDTO = new ClassResponseDTO();
        BeanUtils.copyProperties(_class, responseDTO);
        return responseDTO;
    }

    public void updateClass(String id, ClassRequestDTO request) {
        log.info("Putting class: {}", request.getName());
        Classes classes = classesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        classes.setName(request.getName());
        classes.setDescription(request.getDescription());

        classesRepository.save(classes);
    }

    public void deleteClass(String id) {
        log.info("Deleting class: {}", id);
        Classes classes = classesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        classes.setDeletedAt(LocalDateTime.now());
    }

}
