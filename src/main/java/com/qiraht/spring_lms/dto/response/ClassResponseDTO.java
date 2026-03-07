package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassResponseDTO {
    private String id;
    private String name;
    private String description;
}
