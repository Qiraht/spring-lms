package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponseDTO {
    private String id;
    private String title;
    private String content;
    private String attachment;
    private AuthorDTO author;
}
