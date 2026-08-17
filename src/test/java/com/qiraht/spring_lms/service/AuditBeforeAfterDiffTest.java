package com.qiraht.spring_lms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiraht.spring_lms.AbstractContainerTest;
import com.qiraht.spring_lms.Enum.UserRole;
import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.entity.AuditLog;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.repository.AuditLogRepository;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditBeforeAfterDiffTest extends AbstractContainerTest {

    @Autowired
    private ClassesService classesService;

    @Autowired
    private ClassesRepository classesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void afterStateReflectsUpdatedValuesAfterUpdate() {
        // given
        User actor = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin-" + UUID.randomUUID() + "@test.local")
                .password("encoded")
                .role(UserRole.ADMIN)
                .build());
        setActor(actor);
        Classes saved = classesRepository.save(Classes.builder()
                .name("Original Name")
                .description("original description")
                .build());
        ClassRequestDTO update = new ClassRequestDTO("Updated Name", "updated description");

        // when
        classesService.updateClass(saved.getId().toString(), update);

        // then
        Optional<AuditLog> log = auditLogRepository.findAll().stream()
                .filter(l -> "class".equals(l.getEntityType()) && "update".equals(l.getAction()))
                .findFirst();
        assertThat(log).isPresent();
        assertThat(log.get().getBeforeState()).containsEntry("description", "original description");
        assertThat(log.get().getAfterState()).containsEntry("description", "updated description");
    }

    private void setActor(User user) {
        CustomUsersDetails principal = CustomUsersDetails.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
