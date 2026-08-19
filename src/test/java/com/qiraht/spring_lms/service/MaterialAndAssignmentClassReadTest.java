package com.qiraht.spring_lms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiraht.spring_lms.AbstractContainerTest;
import com.qiraht.spring_lms.Enum.UserRole;
import com.qiraht.spring_lms.dto.response.AssignmentResponseDTO;
import com.qiraht.spring_lms.dto.response.MaterialResponseDTO;
import com.qiraht.spring_lms.entity.Assignment;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.Material;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.repository.AssignmentRepository;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.MaterialRepository;
import com.qiraht.spring_lms.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class MaterialAndAssignmentClassReadTest extends AbstractContainerTest {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getAllMaterialsFromClass_mapsLazyAuthorWithoutLazyInitializationException() {
        UUID classId = classesRepository
                .save(Classes.builder().name("Math").description("Basics").build())
                .getId();
        UUID userId = userRepository.save(user("materials@example.com")).getId();
        materialRepository.save(Material.builder()
                .title("Intro")
                .content("Lesson 1")
                .attachment("notes.pdf")
                .classes(classesRepository.findById(classId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .build());

        Page<MaterialResponseDTO> result =
                materialService.getAllMaterialsFromClass(classId.toString(), Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        MaterialResponseDTO dto = result.getContent().get(0);
        assertThat(dto.getTitle()).isEqualTo("Intro");
        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getFirstName()).isEqualTo("Jane");
    }

    @Test
    void getAssignmentsByClass_mapsLazyAuthorWithoutLazyInitializationException() {
        UUID classId = classesRepository
                .save(Classes.builder().name("Math").description("Basics").build())
                .getId();
        UUID userId = userRepository.save(user("assignments@example.com")).getId();
        assignmentRepository.save(Assignment.builder()
                .title("Homework 1")
                .content("Solve problems")
                .attachment("hw1.pdf")
                .dueDate(LocalDateTime.now().plusDays(7))
                .classes(classesRepository.findById(classId).orElseThrow())
                .user(userRepository.findById(userId).orElseThrow())
                .build());

        Page<AssignmentResponseDTO> result =
                assignmentService.getAssignmentsByClass(classId.toString(), Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        AssignmentResponseDTO dto = result.getContent().get(0);
        assertThat(dto.getTitle()).isEqualTo("Homework 1");
        assertThat(dto.getAuthor()).isNotNull();
        assertThat(dto.getAuthor().getFirstName()).isEqualTo("Jane");
    }

    private User user(String email) {
        return User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email(email)
                .password("encoded-password")
                .role(UserRole.USER)
                .build();
    }
}
