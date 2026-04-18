package com.pharmaApp.user.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcheResponse {

    private String id;
    private String patientUserId;
    private String procheUserId;
    private String relation;

    // ✅ Infos du proche depuis patient_profiles
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private String groupeSanguin;
    private LocalDate dateNaissance;
    private Boolean enceinte;
    private List<String> allergies;
    private List<String> maladiesChroniques;
    private String photoBase64;
    private String qrCode;
}
