package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassesRepository extends JpaRepository<Classes, String> {
    List<Classes> findAll();
}
