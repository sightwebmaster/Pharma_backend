package com.pharmaApp.user.application.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdatePharmacienProfileRequest {
    private String nom;
    private String prenom;
    private String telephone;
    private LocalDate dateNaissance;
    private String numeroOrdre;
    private String specialite;
    private String photoBase64;
}
