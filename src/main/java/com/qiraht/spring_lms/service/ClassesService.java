package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.annotation.Auditable;
import com.qiraht.spring_lms.dto.ProgressReportMessage;
import com.qiraht.spring_lms.dto.request.ClassRequestDTO;
import com.qiraht.spring_lms.dto.request.ExportRequestDTO;
import com.qiraht.spring_lms.dto.response.ClassResponseDTO;
import com.qiraht.spring_lms.entity.Classes;
import com.qiraht.spring_lms.entity.User;
import com.qiraht.spring_lms.exception.AuthenticationException;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.ClassesRepository;
import com.qiraht.spring_lms.repository.UserRepository;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassesService {
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;
    private final ProgressReportPublisher progressReportPublisher;

    @Auditable(entityType = "class", action = "create", idExpr = "#result")
    public UUID createClass(ClassRequestDTO request) {
        log.info("Creating class: {}", request.getName());

        Classes _class = Classes.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return classesRepository.save(_class).getId();
    }

    public Page<ClassResponseDTO> getAllClasses(Pageable pageable) {
        return classesRepository.findAll(pageable).map(classes -> {
            ClassResponseDTO responseDTO = new ClassResponseDTO();
            BeanUtils.copyProperties(classes, responseDTO);
            responseDTO.setId(classes.getId().toString());
            return responseDTO;
        });
    }

    public ClassResponseDTO getClassById(String id) {
        Classes _class = classesRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        ClassResponseDTO responseDTO = new ClassResponseDTO();
        BeanUtils.copyProperties(_class, responseDTO);
        responseDTO.setId(_class.getId().toString());
        return responseDTO;
    }

    @Auditable(entityType = "class", action = "update", idExpr = "#id")
    public void updateClass(String id, ClassRequestDTO request) {
        log.info("Putting class: {}", request.getName());
        Classes classes = classesRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        classes.setName(request.getName());
        classes.setDescription(request.getDescription());

        classesRepository.save(classes);
    }

    @Auditable(entityType = "class", action = "delete", idExpr = "#id")
    public void deleteClass(String id) {
        log.info("Deleting class: {}", id);
        Classes classes = classesRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        classes.setDeletedAt(LocalDateTime.now());
        classesRepository.save(classes);
    }

    @Auditable(entityType = "class", action = "export", idExpr = "#id")
    public void exportProgress(String id, ExportRequestDTO request) {
        Classes _class = classesRepository
                .findById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException("Class with id " + id + " not found"));

        User currentUser = currentUser();

        String recipientEmail = request != null
                        && request.getRecipientEmail() != null
                        && !request.getRecipientEmail().isBlank()
                ? request.getRecipientEmail()
                : currentUser.getEmail();

        progressReportPublisher.publish(ProgressReportMessage.builder()
                .classId(_class.getId())
                .recipientEmail(recipientEmail)
                .requestedAt(LocalDateTime.now())
                .requesterName(currentUser.getFirstName() + " " + currentUser.getLastName())
                .build());
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUsersDetails userDetails) {
            return userRepository
                    .findById(userDetails.getUserId())
                    .orElseThrow(() -> new NotFoundException("Current user not found"));
        }
        throw new AuthenticationException("User not authenticated");
    }
}
