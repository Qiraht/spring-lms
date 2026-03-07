package com.qiraht.spring_lms.dto.request;

import com.qiraht.spring_lms.Enum.ClassRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollRequestDTO {
    @NotBlank
    private UUID userId;

    @NotBlank
    private ClassRole role;
}
