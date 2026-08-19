package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Material;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    @EntityGraph(attributePaths = "user")
    Page<Material> findByClassesId(UUID classesId, Pageable pageable);

    long countByClassesId(UUID classesId);
}
