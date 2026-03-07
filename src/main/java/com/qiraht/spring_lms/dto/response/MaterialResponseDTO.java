package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponseDTO {
    String id;
    String title;
    String content;
    String attachment;
}
