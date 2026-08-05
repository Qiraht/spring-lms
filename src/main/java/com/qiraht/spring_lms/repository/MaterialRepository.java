package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    Page<Material> findByClassesId(UUID classesId, Pageable pageable);

    long countByClassesId(UUID classesId);
}
