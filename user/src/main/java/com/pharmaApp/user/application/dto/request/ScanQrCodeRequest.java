package com.pharmaApp.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO — Requête scan QR Code patient par pharmacien.
 * Le QR Code contient l'UUID Keycloak du patient.
 */
@Data
public class ScanQrCodeRequest {

    @NotBlank(message = "L'ID du patient est obligatoire")
    private String patientUserId;
}