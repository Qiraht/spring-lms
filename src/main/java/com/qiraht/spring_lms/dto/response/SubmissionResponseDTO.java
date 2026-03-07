package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionResponseDTO {
    private String id;
    private String assignmentId;
    private AuthorDTO student;
    private String attachment;
    private BigDecimal score;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
