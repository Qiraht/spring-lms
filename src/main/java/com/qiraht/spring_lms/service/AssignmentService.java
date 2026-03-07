package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.AssignmentRequestDTO;
import com.qiraht.spring_lms.dto.response.AssignmentResponseDTO;
import com.qiraht.spring_lms.entity.Assignment;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.AssignmentRepository;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final ClassesRepository classesRepository;

    public String addAssignment(String classId, AssignmentRequestDTO request) {
        // Check class
        classesRepository.findById(classId).orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        log.info("Adding assignment for class: {}", classId);

        // ID generation with NanoId
        String id = NanoIdUtils.randomNanoId(10);

        Assignment assignment = Assignment.builder()
                .id(id)
                .title(request.getTitle())
                .content(request.getContent())
                .classId(classId)
                .dueDate(request.getDueDate())
                .build();

        assignmentRepository.save(assignment);

        return id;
    }

    public AssignmentResponseDTO getAssignmentById(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        AssignmentResponseDTO response = new AssignmentResponseDTO();

        BeanUtils.copyProperties(assignment, response);

        return response;
    }

    public List<AssignmentResponseDTO> getAssignmentsByClass(String classId) {
        // Check class
        classesRepository.findById(classId).orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        List<Assignment> assignments = assignmentRepository.findByClassId(classId);

        List<AssignmentResponseDTO> response = new ArrayList<>();

        for (Assignment assignment : assignments) {
            AssignmentResponseDTO responseDTO = new AssignmentResponseDTO();
            BeanUtils.copyProperties(assignment, responseDTO);
            response.add(responseDTO);
        }

        return response;
    }

    public void editAssignment(String assignmentId, AssignmentRequestDTO request) {}

    public void deleteAssignment(String assignmentId) {}
}
