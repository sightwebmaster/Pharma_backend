package com.pharmaApp.user.application.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String userId;
    private String role;
    private String nom;
    private String prenom;
    private String email;
    private String qrCode; // ✅ nouveau
}