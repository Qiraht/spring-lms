package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    List<AssignmentSubmission> findByAssignmentId(String assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(String assignmentId, UUID userId);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    long countByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);

    @Query("SELECT AVG(s.score) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    Double getAverageScoreByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);
}
