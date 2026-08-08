package com.qiraht.spring_lms.dto.request;

import com.qiraht.spring_lms.Enum.ClassRole;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollRequestDTO {
    @NotBlank
    private UUID userId;

    @NotBlank
    private ClassRole role;
}
