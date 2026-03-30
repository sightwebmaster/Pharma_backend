package com.pharmaApp.treatement.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO de réponse — Traitement complet.
 * Retourné au Controller après création, modification ou consultation.
 *
 * Calculs inclus (nombrePrises*) : faits dans TraitementMapper
 * depuis les données de l'objet domaine — pas dans le Controller.
 */
public record TraitementResponse(
        String               id,
        String               patientUserId,
        String               pharmacienUserId,
        String               statut,
        LocalDate            dateDebut,
        LocalDate            dateFin,
        String               motif,
        int                  nombrePrisesTotal,
        int                  nombrePrisesConfirmees,
        int                  nombrePrisesManquees,
        List<LigneResponse>  lignes
) {
    public record LigneResponse(
            String          medicamentNom,
            String          principeActif,
            String          dosage,
            int             dureeJours,
            List<LocalTime> heuresPrise
    ) {}
}