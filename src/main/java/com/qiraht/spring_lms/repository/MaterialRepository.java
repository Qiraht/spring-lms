package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, String> {
    List<Material> findByClassId(String classId);
}
