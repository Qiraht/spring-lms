package com.qiraht.spring_lms.service;

import static java.util.stream.Collectors.toMap;

import com.qiraht.spring_lms.dto.response.AuthorDTO;
import com.qiraht.spring_lms.dto.response.StudentClassSummaryDTO;
import com.qiraht.spring_lms.entity.Enrollment;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.entity.StudentProgress;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.AuthorizationException;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.*;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository.SubmissionAvgView;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository.SubmissionCountView;
import com.qiraht.spring_lms.repository.StudentProgressRepository.MaterialCompletionView;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private final AuditService auditService;

    public void markMaterialAsCompleted(String materialId) {
        Material material = materialRepository
                .findById(UUID.fromString(materialId))
                .orElseThrow(() -> new NotFoundException("Material not found"));

        CustomUsersDetails userDetails = (CustomUsersDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository
                .findById(userDetails.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Only track progress for enrolled students
        if (!enrollmentRepository.existsByClassesIdAndUserIdAndRole(
                material.getClasses().getId(), user.getId(), com.qiraht.spring_lms.Enum.ClassRole.STUDENT)) {
            return;
        }

        Optional<StudentProgress> existingProgress =
                progressRepository.findByUserIdAndMaterialId(user.getId(), UUID.fromString(materialId));

        if (existingProgress.isEmpty()) {
            StudentProgress progress = StudentProgress.builder()
                    .user(user)
                    .material(material)
                    .isCompleted(true)
                    .build();
            progressRepository.save(progress);
            auditService.record(
                    user.getId(),
                    "progress",
                    progress.getId(),
                    "create",
                    "success",
                    null,
                    auditService.snapshot(progress));
        }
    }

    public StudentClassSummaryDTO getStudentClassSummary(String classId, UUID studentId) {
        UUID classUuid = UUID.fromString(classId);

        classesRepository.findById(classUuid).orElseThrow(() -> new NotFoundException("Class not found"));

        User student = userRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student not found"));

        // Check enrollment
        if (!enrollmentRepository.existsByClassesIdAndUserIdAndRole(
                classUuid, studentId, com.qiraht.spring_lms.Enum.ClassRole.STUDENT)) {
            throw new AuthorizationException("User is not enrolled as a student in this class");
        }

        // Aggregate Metrics
        Integer totalMaterials = (int) materialRepository.countByClassesId(classUuid);
        Integer completedMaterials =
                (int) progressRepository.countCompletedMaterialsByUserIdAndClassId(studentId, classUuid);

        Integer totalAssignments = (int) assignmentRepository.countByClassesId(classUuid);
        Integer submittedAssignments = (int) submissionRepository.countByUserIdAndClassId(studentId, classUuid);

        Double averageScoreDouble = submissionRepository.getAverageScoreByUserIdAndClassId(studentId, classUuid);
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

    public Page<StudentClassSummaryDTO> getAllStudentSummariesForClass(String classId, Pageable pageable) {
        UUID classUuid = UUID.fromString(classId);

        classesRepository.findById(classUuid).orElseThrow(() -> new NotFoundException("Class not found"));

        int totalMaterials = (int) materialRepository.countByClassesId(classUuid);
        int totalAssignments = (int) assignmentRepository.countByClassesId(classUuid);

        Map<UUID, Long> completedMaterialsByUser =
                progressRepository.countCompletedMaterialsForClass(classUuid).stream()
                        .collect(toMap(MaterialCompletionView::getUserId, MaterialCompletionView::getCompleted));
        Map<UUID, Long> submittedByUser = submissionRepository.countSubmissionsForClass(classUuid).stream()
                .collect(toMap(SubmissionCountView::getUserId, SubmissionCountView::getCount));
        Map<UUID, Double> avgScoreByUser = submissionRepository.avgScoresForClass(classUuid).stream()
                .collect(toMap(SubmissionAvgView::getUserId, SubmissionAvgView::getAvgScore));

        Page<Enrollment> enrollments = enrollmentRepository.findByClassesIdAndRole(
                classUuid, com.qiraht.spring_lms.Enum.ClassRole.STUDENT, pageable);

        return enrollments.map(enrollment -> {
            User student = enrollment.getUser();
            UUID studentId = student.getId();

            int completedMaterials =
                    completedMaterialsByUser.getOrDefault(studentId, 0L).intValue();
            int submittedAssignments =
                    submittedByUser.getOrDefault(studentId, 0L).intValue();
            Double avg = avgScoreByUser.get(studentId);
            BigDecimal averageScore =
                    avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

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
        });
    }
}
