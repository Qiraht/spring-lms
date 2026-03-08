package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    Page<Assignment> findByClassesId(String classesId, Pageable pageable);

    long countByClassesId(String classesId);
}
