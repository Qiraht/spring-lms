package com.qiraht.spring_lms.aspect;

import com.qiraht.spring_lms.annotation.Auditable;
import com.qiraht.spring_lms.security.CustomUsersDetails;
import com.qiraht.spring_lms.service.AuditService;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        UUID actorId = currentUserId();

        Map<String, Object> beforeState = null;
        UUID entityId = null;

        try {
            entityId = resolveId(joinPoint, auditable.idExpr(), null);
            beforeState = auditService.snapshotById(auditable.entityType(), entityId);

            Object result = joinPoint.proceed();

            if (entityId == null) {
                entityId = resolveId(joinPoint, auditable.idExpr(), result);
            }
            Map<String, Object> afterState = auditService.snapshotById(auditable.entityType(), entityId);

            UUID effectiveActor = actorId != null ? actorId : entityId;
            String status = "success";
            if ("create".equals(auditable.action())) {
                beforeState = null;
            } else if ("delete".equals(auditable.action())) {
                afterState = null;
            }
            auditService.record(
                    effectiveActor,
                    auditable.entityType(),
                    entityId,
                    auditable.action(),
                    status,
                    beforeState,
                    afterState);
            return result;
        } catch (Throwable ex) {
            log.warn(
                    "Recording failed action {} on {} by actor {}",
                    auditable.action(),
                    auditable.entityType(),
                    actorId);
            auditService.record(
                    actorId, auditable.entityType(), entityId, auditable.action(), "failed", beforeState, null);
            throw ex;
        }
    }

    private UUID resolveId(ProceedingJoinPoint joinPoint, String idExpr, Object result) {
        if (idExpr == null || idExpr.isBlank()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            StandardEvaluationContext context = new StandardEvaluationContext();
            Object[] args = joinPoint.getArgs();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (i < args.length) {
                    context.setVariable(parameters[i].getName(), args[i]);
                }
            }
            context.setVariable("result", result);

            Object value = PARSER.parseExpression(idExpr).getValue(context);
            return toUuid(value);
        } catch (Exception e) {
            log.debug("Could not resolve id expression '{}': {}", idExpr, e.getMessage());
            return null;
        }
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUsersDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }
}
