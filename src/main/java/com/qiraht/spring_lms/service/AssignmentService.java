package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.request.AssignmentRequestDTO;
import com.qiraht.spring_lms.dto.response.AssignmentResponseDTO;
import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.entity.Assignment;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.AssignmentRepository;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import com.soundicly.jnanoidenhanced.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;

    public String addAssignment(String classId, AssignmentRequestDTO request) {
        // Check class
        Classes classes = classesRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        log.info("Adding assignment for class: {}", classId);

        // ID generation with NanoId
        String id = NanoIdUtils.randomNanoId(10);

        Assignment assignment = Assignment.builder()
                .id(id)
                .title(request.getTitle())
                .content(request.getContent())
                .classes(classes)
                .user(user)
                .dueDate(request.getDueDate())
                .build();

        assignmentRepository.save(assignment);

        return id;
    }

    public AssignmentResponseDTO getAssignmentById(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        AssignmentResponseDTO response = new AssignmentResponseDTO();

        BeanUtils.copyProperties(assignment, response);
        if (assignment.getUser() != null) {
            response.setAuthor(AuthorDTO.builder()
                    .id(assignment.getUser().getId())
                    .firstName(assignment.getUser().getFirstName())
                    .lastName(assignment.getUser().getLastName())
                    .build());
        }

        return response;
    }

    public Page<AssignmentResponseDTO> getAssignmentsByClass(String classId, Pageable pageable) {
        // Check class
        classesRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        Page<Assignment> assignments = assignmentRepository.findByClassesId(classId, pageable);

        return assignments.map(assignment -> {
            AssignmentResponseDTO responseDTO = new AssignmentResponseDTO();
            BeanUtils.copyProperties(assignment, responseDTO);
            if (assignment.getUser() != null) {
                responseDTO.setAuthor(AuthorDTO.builder()
                        .id(assignment.getUser().getId())
                        .firstName(assignment.getUser().getFirstName())
                        .lastName(assignment.getUser().getLastName())
                        .build());
            }
            return responseDTO;
        });
    }

    public String editAssignment(String assignmentId, AssignmentRequestDTO request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        assignment.setTitle(request.getTitle());
        assignment.setContent(request.getContent());
        assignment.setAttachment(request.getAttachment());
        assignment.setDueDate(request.getDueDate());

        assignmentRepository.save(assignment);

        return assignment.getId();
    }

    public String deleteAssignment(String assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        assignment.setDeletedAt(LocalDateTime.now());

        assignmentRepository.save(assignment);

        return assignment.getId();
    }
}
