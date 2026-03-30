package com.pharmaApp.user.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
@Getter
@Setter

public class PatientProfileResponse {

    private String id;
    private String userId;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String telephone;
    private String groupeSanguin;
    private List<String> allergies;
    private List<String> maladiesChroniques;
    private LocalDateTime createdAt;
    private String qrCode;

    // Getters & Setters

}