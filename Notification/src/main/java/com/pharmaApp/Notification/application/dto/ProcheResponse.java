package com.pharmaApp.Notification.application.dto;

import lombok.Data;

@Data
public class ProcheResponse {
    private String id;
    private String patientUserId;
    private String procheUserId;
    private String relation;
    private String nom;
    private String prenom;
    private String email;
}
