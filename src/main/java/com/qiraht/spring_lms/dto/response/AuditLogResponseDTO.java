package com.qiraht.spring_lms.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponseDTO {
    private String id;
    private String userId;
    private String entityType;
    private String entityId;
    private String action;
    private String status;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private LocalDateTime createdAt;
}
