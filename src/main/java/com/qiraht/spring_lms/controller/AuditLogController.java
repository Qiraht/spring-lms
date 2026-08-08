package com.qiraht.spring_lms.controller;

import com.qiraht.spring_lms.dto.ApiResponse;
import com.qiraht.spring_lms.dto.response.AuditLogResponseDTO;
import com.qiraht.spring_lms.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Validated
public class AuditLogController {
    private final AuditService auditService;

    @Tag(name = "Audit Log")
    @Operation(
            summary = "Get Audit Logs",
            description = "Return audit logs with optional filters. Authentication Needed and role 'ADMIN' needed")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getAuditLogs(
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponseDTO> data =
                auditService.toPageDTO(auditService.search(entityType, action, status, userId, from, to, pageable));

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }

    @Tag(name = "Audit Log")
    @Operation(
            summary = "Get Audit Log By Id",
            description = "Return an audit log by id. Authentication Needed and role 'ADMIN' needed")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuditLogResponseDTO>> getAuditLogById(@PathVariable("id") String id) {
        AuditLogResponseDTO data = auditService.toDTO(auditService.getById(UUID.fromString(id)));

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK.value(), "success", data));
    }
}
