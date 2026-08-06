package com.qiraht.spring_lms.service;

import com.qiraht.spring_lms.dto.response.AuditLogResponseDTO;
import com.qiraht.spring_lms.entity.AuditLog;
import com.qiraht.spring_lms.exception.NotFoundException;
import com.qiraht.spring_lms.repository.AuditLogRepository;
import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID userId,
            String entityType,
            UUID entityId,
            String action,
            String status,
            Map<String, Object> beforeState,
            Map<String, Object> afterState) {
        if (entityId == null) {
            log.warn("Skipping audit record for action {} on {}: entity id could not be resolved", action, entityType);
            return;
        }
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .status(status)
                .beforeState(beforeState)
                .afterState(afterState)
                .build();
        auditLogRepository.save(auditLog);
    }

    public Map<String, Object> snapshotById(String entityType, UUID id) {
        if (id == null) {
            return null;
        }
        Class<?> entityClass = EntityTypeRegistry.forName(entityType);
        if (entityClass == null) {
            log.warn("No entity class registered for entity type {}", entityType);
            return null;
        }
        Object entity = entityManager.find(entityClass, id);
        return snapshot(entity);
    }

    public Map<String, Object> snapshot(Object entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> state = new LinkedHashMap<>();
        for (Class<?> clazz = entity.getClass();
                clazz != null && clazz != Object.class;
                clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (isAssociation(field)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(entity);
                    if (value != null) {
                        state.put(field.getName(), normalize(value));
                    }
                } catch (IllegalAccessException e) {
                    log.warn(
                            "Could not read field {} on {}",
                            field.getName(),
                            entity.getClass().getSimpleName(),
                            e);
                }
            }
        }
        return state.isEmpty() ? null : state;
    }

    public AuditLog getById(UUID id) {
        return auditLogRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Audit log with id " + id + " not found"));
    }

    public AuditLogResponseDTO toDTO(AuditLog auditLog) {
        return AuditLogResponseDTO.builder()
                .id(auditLog.getId().toString())
                .userId(auditLog.getUserId() != null ? auditLog.getUserId().toString() : null)
                .entityType(auditLog.getEntityType())
                .entityId(
                        auditLog.getEntityId() != null ? auditLog.getEntityId().toString() : null)
                .action(auditLog.getAction())
                .status(auditLog.getStatus())
                .beforeState(auditLog.getBeforeState())
                .afterState(auditLog.getAfterState())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    public Page<AuditLogResponseDTO> toPageDTO(Page<AuditLog> auditLogs) {
        return auditLogs.map(this::toDTO);
    }

    public Page<AuditLog> search(
            String entityType,
            String action,
            String status,
            UUID userId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (from != null) {
            LocalDateTime fromAt = from.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromAt));
        }
        if (to != null) {
            LocalDateTime toAt = to.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toAt));
        }

        return auditLogRepository.findAll(spec, pageable);
    }

    private boolean isAssociation(Field field) {
        for (Class<? extends java.lang.annotation.Annotation> annotation :
                List.of(ManyToOne.class, OneToMany.class, OneToOne.class, ManyToMany.class)) {
            if (field.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    private Object normalize(Object value) {
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().toString();
        }
        return value;
    }

    private static final class EntityTypeRegistry {
        private static final Map<String, Class<?>> TYPES = new HashMap<>();

        static {
            TYPES.put("user", com.qiraht.spring_lms.entity.User.class);
            TYPES.put("class", com.qiraht.spring_lms.entity.Classes.class);
            TYPES.put("material", com.qiraht.spring_lms.entity.Material.class);
            TYPES.put("assignment", com.qiraht.spring_lms.entity.Assignment.class);
            TYPES.put("enrollment", com.qiraht.spring_lms.entity.Enrollment.class);
            TYPES.put("submission", com.qiraht.spring_lms.entity.AssignmentSubmission.class);
            TYPES.put("progress", com.qiraht.spring_lms.entity.StudentProgress.class);
        }

        private EntityTypeRegistry() {}

        static Class<?> forName(String name) {
            return TYPES.get(name);
        }
    }
}
