package com.pharmaApp.treatement.application.dto;

import java.time.LocalDateTime;

/**
 * DTO de réponse — une prise planifiée.
 * Utilisé dans l'écran de suivi patient.
 */
public record PriseResponse(
        String        id,
        String        idLigneMedicament,
        String        medicamentNom,
        LocalDateTime heureReelle,
        String        statut
) {}