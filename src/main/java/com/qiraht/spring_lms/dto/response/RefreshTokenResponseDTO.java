package com.qiraht.spring_lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponseDTO {
    private String email;
    private String accessToken;
    private long expiresIn;
    private String refreshToken;
}
