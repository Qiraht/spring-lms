package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.ClassRequestDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<Classes> getAllClasses() {
        return classesRepository.findAll();
    }

    public Classes getClassById(String id) {
        Classes classes = new Classes();

        classesRepository.findById(id).ifPresentOrElse(
                c -> { BeanUtils.copyProperties(c, classes);},
                () -> { throw new RuntimeException("Class with id " + id + " not found");}
        );

        return classes;
    }


}
