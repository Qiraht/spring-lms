package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    Page<AssignmentSubmission> findByAssignmentId(String assignmentId, Pageable pageable);

    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(String assignmentId, UUID userId);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    long countByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);

    @Query("SELECT AVG(s.score) FROM AssignmentSubmission s WHERE s.user.id = :userId AND s.assignment.classes.id = :classId")
    Double getAverageScoreByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);
}
