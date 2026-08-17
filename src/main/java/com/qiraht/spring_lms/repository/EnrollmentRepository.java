package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.Enum.ClassRole;
import com.qiraht.spring_lms.entity.Enrollment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    boolean existsByClassesIdAndUserIdAndRole(UUID classId, UUID userId, ClassRole role);

    boolean existsByClassesIdAndUserId(UUID classId, UUID userId);

    java.util.Optional<Enrollment> findByClassesIdAndUserId(UUID classId, UUID userId);

    @EntityGraph(attributePaths = "user")
    Page<Enrollment> findByClassesIdAndRole(UUID classId, ClassRole role, Pageable pageable);
}
