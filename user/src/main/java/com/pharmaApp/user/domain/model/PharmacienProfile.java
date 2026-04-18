package com.pharmaApp.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacienProfile {

    private String id;
    private String userId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private String numeroOrdre;
    private String specialite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String photoBase64;
    private String qrCode;
}
