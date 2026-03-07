package com.qiraht.spring_lms.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequestDTO {
    @NotBlank
    String title;
    @NotBlank
    String content;
    String attachment;
    @NotNull
    @FutureOrPresent
    LocalDateTime dueDate;
}
