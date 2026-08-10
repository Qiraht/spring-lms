package com.qiraht.spring_lms.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReportMessage {
    private UUID classId;
    private String recipientEmail;
    private LocalDateTime requestedAt;
    private String requesterName;
}
