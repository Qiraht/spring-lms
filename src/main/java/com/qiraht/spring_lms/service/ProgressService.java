package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.dto.response.StudentClassSummaryDTO;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.entity.StudentProgress;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.*;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

        private final StudentProgressRepository progressRepository;
        private final AssignmentSubmissionRepository submissionRepository;
        private final MaterialRepository materialRepository;
        private final AssignmentRepository assignmentRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final UserRepository userRepository;
        private final ClassesRepository classesRepository;

        public void markMaterialAsCompleted(String materialId) {
                Material material = materialRepository.findById(materialId)
                                .orElseThrow(() -> new NotFoundException("Material not found"));

                CustomUsersDetails userDetails = (CustomUsersDetails) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getUserId())
                                .orElseThrow(() -> new NotFoundException("User not found"));

                // Only track progress for enrolled students
                if (!enrollmentRepository.existsByClassesIdAndUserIdAndRole(material.getClasses().getId(), user.getId(),
                                com.qiraht.spring_lms.Enum.ClassRole.STUDENT)) {
                        return;
                }

                Optional<StudentProgress> existingProgress = progressRepository.findByUserIdAndMaterialId(user.getId(),
                                materialId);

                if (existingProgress.isEmpty()) {
                        StudentProgress progress = StudentProgress.builder()
                                        .user(user)
                                        .material(material)
                                        .isCompleted(true)
                                        .build();
                        progressRepository.save(progress);
                }
        }

        public StudentClassSummaryDTO getStudentClassSummary(String classId, UUID studentId) {
                classesRepository.findById(classId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                User student = userRepository.findById(studentId)
                                .orElseThrow(() -> new NotFoundException("Student not found"));

                // Check enrollment
                if (!enrollmentRepository.existsByClassesIdAndUserIdAndRole(classId, studentId,
                                com.qiraht.spring_lms.Enum.ClassRole.STUDENT)) {
                        throw new RuntimeException("User is not enrolled as a student in this class");
                }

                // Aggregate Metrics
                Integer totalMaterials = materialRepository.findByClassesId(classId).size();
                Integer completedMaterials = (int) progressRepository.countCompletedMaterialsByUserIdAndClassId(
                                studentId,
                                classId);

                Integer totalAssignments = assignmentRepository.findByClassesId(classId).size();
                Integer submittedAssignments = (int) submissionRepository.countByUserIdAndClassId(studentId, classId);

                Double averageScoreDouble = submissionRepository.getAverageScoreByUserIdAndClassId(studentId, classId);
                BigDecimal averageScore = averageScoreDouble != null
                                ? BigDecimal.valueOf(averageScoreDouble).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                int totalTrackableItems = totalMaterials + totalAssignments;
                int totalCompletedItems = completedMaterials + submittedAssignments;

                Double completionPercentage = 0.0;
                if (totalTrackableItems > 0) {
                        completionPercentage = ((double) totalCompletedItems / totalTrackableItems) * 100.0;
                }

                return StudentClassSummaryDTO.builder()
                                .classId(classId)
                                .student(AuthorDTO.builder()
                                                .id(student.getId())
                                                .firstName(student.getFirstName())
                                                .lastName(student.getLastName())
                                                .build())
                                .totalMaterials(totalMaterials)
                                .completedMaterials(completedMaterials)
                                .totalAssignments(totalAssignments)
                                .submittedAssignments(submittedAssignments)
                                .averageScore(averageScore)
                                .completionPercentage(completionPercentage)
                                .build();
        }

        public List<StudentClassSummaryDTO> getAllStudentSummariesForClass(String classId) {
                classesRepository.findById(classId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                // Get all enrolled students
                // To do this, we need a method in EnrollmentRepository to find users by class
                // and role.
                // We will fetch all enrollments and filter for now if repository method isn't
                // built.
                List<com.qiraht.spring_lms.entity.Enrollment> enrollments = enrollmentRepository.findAll().stream()
                                .filter(e -> e.getClasses().getId().equals(classId)
                                                && e.getRole().equals(com.qiraht.spring_lms.Enum.ClassRole.STUDENT))
                                .toList();

                List<StudentClassSummaryDTO> summaries = new ArrayList<>();
                for (com.qiraht.spring_lms.entity.Enrollment enrollment : enrollments) {
                        summaries.add(getStudentClassSummary(classId, enrollment.getUser().getId()));
                }

                return summaries;
        }
}
