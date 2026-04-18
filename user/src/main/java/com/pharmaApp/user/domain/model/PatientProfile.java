package com.pharmaApp.user.domain.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    private String id;
    private String userId;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private String groupeSanguin;
    private Boolean enceinte;
    private List<String> allergies;
    private List<String> maladiesChroniques;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String qrCode;
    private String photoBase64;


}
