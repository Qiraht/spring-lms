package com.qiraht.spring_lms.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
