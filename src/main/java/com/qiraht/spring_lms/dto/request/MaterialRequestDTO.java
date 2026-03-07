package com.qiraht.spring_lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequestDTO {
    @NotBlank
    String title;

    @NotBlank
    String content;

    String attachment;
}
