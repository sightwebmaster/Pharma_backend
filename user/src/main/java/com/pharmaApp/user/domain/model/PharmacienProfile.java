package com.pharmaApp.user.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacienProfile {


    private String          id;
    private String        userId;          // ID venant de auth-service
    private String        nom;
    private String        prenom;
    private String        email;
    private String        telephone;
    private String        numeroOrdre;     // numéro ordre pharmacien
    private String        specialite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}