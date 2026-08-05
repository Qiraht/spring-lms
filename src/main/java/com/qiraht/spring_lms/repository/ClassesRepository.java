package com.qiraht.spring_lms.repository;

import com.qiraht.spring_lms.entity.Classes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassesRepository extends JpaRepository<Classes, UUID> {}
