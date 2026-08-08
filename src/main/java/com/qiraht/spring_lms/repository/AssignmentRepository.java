package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Assignment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    Page<Assignment> findByClassesId(UUID classesId, Pageable pageable);

    long countByClassesId(UUID classesId);
}
