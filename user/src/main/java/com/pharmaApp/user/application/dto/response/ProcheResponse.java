package com.pharmaApp.user.application.dto.response;

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
}