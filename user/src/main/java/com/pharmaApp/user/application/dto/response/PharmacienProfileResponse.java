package com.pharmaApp.user.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;
@Getter
@Setter

public class PharmacienProfileResponse {

    private String id;
    private String userId;
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private LocalDate dateNaissance;
    private String numeroOrdre;
    private String specialite;
    private LocalDateTime createdAt;
    private String photoBase64;
    private String qrCode;

    // Getters & Setters

}
