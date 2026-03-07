package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import com.qiraht.spring_lms.Enum.ClassRole;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByClassesIdAndUserIdAndRole(String classId, UUID userId, ClassRole role);

    boolean existsByClassesIdAndUserId(String classId, UUID userId);

    java.util.Optional<Enrollment> findByClassesIdAndUserId(String classId, UUID userId);
}
