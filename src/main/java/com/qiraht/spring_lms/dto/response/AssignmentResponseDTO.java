package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentResponseDTO {
    private String id;
    private String title;
    private String content;
    private String attachment;
    private LocalDateTime dueDate;
    private AuthorDTO author;
}
