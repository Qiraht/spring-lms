package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentClassSummaryDTO {
    private String classId;
    private AuthorDTO student;
    private Integer totalMaterials;
    private Integer completedMaterials;
    private Integer totalAssignments;
    private Integer submittedAssignments;
    private BigDecimal averageScore;
    private Double completionPercentage;
}
