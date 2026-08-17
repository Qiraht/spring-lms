package com.qiraht.spring_lms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qiraht.spring_lms.Enum.ClassRole;
import com.qiraht.spring_lms.dto.response.StudentClassSummaryDTO;
import com.qiraht.spring_lms.entity.Enrollment;
import com.qiraht.spring_lms.repository.AssignmentRepository;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository.SubmissionAvgView;
import com.qiraht.spring_lms.repository.AssignmentSubmissionRepository.SubmissionCountView;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.EnrollmentRepository;
import com.qiraht.spring_lms.repository.MaterialRepository;
import com.qiraht.spring_lms.repository.StudentProgressRepository;
import com.qiraht.spring_lms.repository.StudentProgressRepository.MaterialCompletionView;
import com.qiraht.spring_lms.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private StudentProgressRepository progressRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassesRepository classesRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void getAllStudentSummaries_usesBatchQueriesInsteadOfPerStudentLoop() {
        // given
        UUID classId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Enrollment enrollment = Enrollment.builder().user(mockUser(studentId)).build();

        when(classesRepository.findById(classId))
                .thenReturn(Optional.of(com.qiraht.spring_lms.entity.Classes.builder()
                        .id(classId)
                        .build()));
        when(materialRepository.countByClassesId(classId)).thenReturn(10L);
        when(assignmentRepository.countByClassesId(classId)).thenReturn(5L);
        when(progressRepository.countCompletedMaterialsForClass(classId))
                .thenReturn(List.of(materialCompletion(studentId, 4L)));
        when(submissionRepository.countSubmissionsForClass(classId))
                .thenReturn(List.of(submissionCount(studentId, 3L)));
        when(submissionRepository.avgScoresForClass(classId)).thenReturn(List.of(submissionAvg(studentId, 85.0)));
        when(enrollmentRepository.findByClassesIdAndRole(classId, ClassRole.STUDENT, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(enrollment)));

        // when
        Page<StudentClassSummaryDTO> result =
                progressService.getAllStudentSummariesForClass(classId.toString(), Pageable.unpaged());

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        StudentClassSummaryDTO summary = result.getContent().get(0);
        assertThat(summary.getTotalMaterials()).isEqualTo(10);
        assertThat(summary.getCompletedMaterials()).isEqualTo(4);
        assertThat(summary.getTotalAssignments()).isEqualTo(5);
        assertThat(summary.getSubmittedAssignments()).isEqualTo(3);
        assertThat(summary.getAverageScore().doubleValue()).isEqualTo(85.0);

        // batch queries are invoked once for the whole class, not per student
        verify(progressRepository, times(1)).countCompletedMaterialsForClass(classId);
        verify(submissionRepository, times(1)).countSubmissionsForClass(classId);
        verify(submissionRepository, times(1)).avgScoresForClass(classId);
    }

    private com.qiraht.spring_lms.entity.User mockUser(UUID id) {
        return com.qiraht.spring_lms.entity.User.builder()
                .id(id)
                .firstName("Jane")
                .lastName("Doe")
                .build();
    }

    private MaterialCompletionView materialCompletion(UUID userId, Long completed) {
        return new MaterialCompletionView() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public Long getCompleted() {
                return completed;
            }
        };
    }

    private SubmissionCountView submissionCount(UUID userId, Long count) {
        return new SubmissionCountView() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private SubmissionAvgView submissionAvg(UUID userId, Double avg) {
        return new SubmissionAvgView() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public Double getAvgScore() {
                return avg;
            }
        };
    }
}
