package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    Page<AssignmentSubmission> findByAssignmentId(UUID assignmentId, Pageable pageable);

    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    @Query(
            "SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    long countByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") UUID classId);

    @Query(
            "SELECT AVG(s.score) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    Double getAverageScoreByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") UUID classId);

    @Query(
            """
            SELECT s.user.id AS userId, COUNT(s) AS count
            FROM AssignmentSubmission s
            WHERE s.assignment.classes.id = :classId
            GROUP BY s.user.id
            """)
    List<SubmissionCountView> countSubmissionsForClass(@Param("classId") UUID classId);

    @Query(
            """
            SELECT s.user.id AS userId, AVG(s.score) AS avgScore
            FROM AssignmentSubmission s
            WHERE s.assignment.classes.id = :classId
            GROUP BY s.user.id
            """)
    List<SubmissionAvgView> avgScoresForClass(@Param("classId") UUID classId);

    interface SubmissionCountView {
        UUID getUserId();

        Long getCount();
    }

    interface SubmissionAvgView {
        UUID getUserId();

        Double getAvgScore();
    }
}
