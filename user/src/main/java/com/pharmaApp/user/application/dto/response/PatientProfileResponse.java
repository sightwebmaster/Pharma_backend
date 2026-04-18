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
    private Boolean enceinte;
    private List<String> allergies;
    private List<String> maladiesChroniques;
    private LocalDateTime createdAt;
    private String qrCode;
    private String email;
    private String photoBase64;

    // Getters & Setters

}
