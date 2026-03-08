package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MaterialRepository extends JpaRepository<Material, String> {
    Page<Material> findByClassesId(String classesId, Pageable pageable);

    long countByClassesId(String classesId);
}
