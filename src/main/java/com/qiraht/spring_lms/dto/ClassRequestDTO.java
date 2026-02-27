package com.qiraht.spring_lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassRequestDTO {
    @NotBlank(message = "Class name is required")
    private String name;

    private String description;
}
