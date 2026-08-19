package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.annotation.Auditable;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;

    @Auditable(entityType = "assignment", action = "create", idExpr = "#result")
    public String addAssignment(String classId, AssignmentRequestDTO request) {
        // Check class
        Classes classes = classesRepository
                .findById(UUID.fromString(classId))
                .orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        log.info("Adding assignment for class: {}", classId);

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .classes(classes)
                .user(user)
                .dueDate(request.getDueDate())
                .build();

        assignmentRepository.save(assignment);

        return assignment.getId().toString();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDTO getAssignmentById(String assignmentId) {
        Assignment assignment = assignmentRepository
                .findById(UUID.fromString(assignmentId))
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        AssignmentResponseDTO response = new AssignmentResponseDTO();

        BeanUtils.copyProperties(assignment, response);
        response.setId(assignment.getId().toString());
        if (assignment.getUser() != null) {
            response.setAuthor(AuthorDTO.builder()
                    .id(assignment.getUser().getId())
                    .firstName(assignment.getUser().getFirstName())
                    .lastName(assignment.getUser().getLastName())
                    .build());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Page<AssignmentResponseDTO> getAssignmentsByClass(String classId, Pageable pageable) {
        // Check class
        classesRepository
                .findById(UUID.fromString(classId))
                .orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        Page<Assignment> assignments = assignmentRepository.findByClassesId(UUID.fromString(classId), pageable);

        return assignments.map(assignment -> {
            AssignmentResponseDTO responseDTO = new AssignmentResponseDTO();
            BeanUtils.copyProperties(assignment, responseDTO);
            responseDTO.setId(assignment.getId().toString());
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

    @Auditable(entityType = "assignment", action = "update", idExpr = "#assignmentId")
    public String editAssignment(String assignmentId, AssignmentRequestDTO request) {
        Assignment assignment = assignmentRepository
                .findById(UUID.fromString(assignmentId))
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        assignment.setTitle(request.getTitle());
        assignment.setContent(request.getContent());
        assignment.setAttachment(request.getAttachment());
        assignment.setDueDate(request.getDueDate());

        assignmentRepository.save(assignment);

        return assignment.getId().toString();
    }

    @Auditable(entityType = "assignment", action = "delete", idExpr = "#assignmentId")
    public String deleteAssignment(String assignmentId) {
        Assignment assignment = assignmentRepository
                .findById(UUID.fromString(assignmentId))
                .orElseThrow(() -> new NotFoundException("Assignment with id " + assignmentId + " not found"));

        assignment.setDeletedAt(LocalDateTime.now());

        assignmentRepository.save(assignment);

        return assignment.getId().toString();
    }
}
