package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.entity.*;
import com.qiraht.spring_lms.repository.*;
import com.qiraht.spring_lms.Enum.ClassRole;
import com.qiraht.spring_lms.dto.request.EnrollRequestDTO;
import com.qiraht.spring_lms.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;

    private final MaterialRepository materialRepository;
    private final AssignmentRepository assignmentRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;

    public boolean isTeacherOfClass(UUID userId, String classId) {
        return enrollmentRepository.existsByClassesIdAndUserIdAndRole(UUID.fromString(classId), userId, ClassRole.TEACHER);
    }

    public boolean isTeacherOfMaterial(UUID userId, String materialId) {
        Material material = materialRepository.findById(UUID.fromString(materialId))
                .orElseThrow(() -> new NotFoundException("Material not found"));
        return isTeacherOfClass(userId, material.getClasses().getId().toString());
    }

    public boolean isTeacherOfAssignment(UUID userId, String assignmentId) {
        Assignment assignment = assignmentRepository.findById(UUID.fromString(assignmentId))
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        return isTeacherOfClass(userId, assignment.getClasses().getId().toString());
    }

    public boolean isStudentOfClass(UUID userId, String classId) {
        return enrollmentRepository.existsByClassesIdAndUserIdAndRole(UUID.fromString(classId), userId, ClassRole.STUDENT);
    }

    public boolean isEnrolledInClass(UUID userId, String classId) {
        return enrollmentRepository.existsByClassesIdAndUserId(UUID.fromString(classId), userId);
    }

    @Transactional
    public void enrollUsers(String classId, List<EnrollRequestDTO> requests) {
        Classes classes = classesRepository.findById(UUID.fromString(classId))
                .orElseThrow(() -> new NotFoundException("Class with id " + classId + " not found"));

        List<Enrollment> enrollments = new ArrayList<>();
        for (EnrollRequestDTO request : requests) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("User with id " + request.getUserId() + " not found"));

            // Check if already enrolled
            if (enrollmentRepository.existsByClassesIdAndUserId(UUID.fromString(classId), request.getUserId())) {
                continue; // Or update role, or throw exception. Proceeding with skipping for now.
            }

            Enrollment enrollment = Enrollment.builder()
                    .classes(classes)
                    .user(user)
                    .role(request.getRole())
                    .build();
            enrollments.add(enrollment);
        }

        enrollmentRepository.saveAll(enrollments);
    }

    @Transactional
    public void removeUserFromClass(String classId, UUID userId) {
        Enrollment enrollment = enrollmentRepository.findByClassesIdAndUserId(UUID.fromString(classId), userId)
                .orElseThrow(() -> new NotFoundException("User is not enrolled in this class"));

        enrollment.setDeletedAt(java.time.LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }
}
