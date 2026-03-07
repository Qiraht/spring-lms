package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StudentProgressRepository extends JpaRepository<StudentProgress, UUID> {
    Optional<StudentProgress> findByUserIdAndMaterialId(UUID userId, String materialId);

    Optional<StudentProgress> findByUserIdAndAssignmentId(UUID userId, String assignmentId);

    @Query("SELECT COUNT(p) FROM StudentProgress p WHERE p.user.id = :userId AND p.material.classes.id = :classId AND p.isCompleted = true")
    long countCompletedMaterialsByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);

    @Query("SELECT COUNT(p) FROM StudentProgress p WHERE p.user.id = :userId AND p.assignment.classes.id = :classId AND p.isCompleted = true")
    long countCompletedAssignmentsByUserIdAndClassId(@Param("userId") UUID userId, @Param("classId") String classId);
}
