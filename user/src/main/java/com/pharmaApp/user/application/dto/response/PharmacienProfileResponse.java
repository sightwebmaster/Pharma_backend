package com.pharmaApp.user.application.dto.response;

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
    private String numeroOrdre;
    private String specialite;
    private LocalDateTime createdAt;

    // Getters & Setters

}