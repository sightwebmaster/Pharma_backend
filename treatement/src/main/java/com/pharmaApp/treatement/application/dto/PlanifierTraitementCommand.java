package com.pharmaApp.treatement.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Commande de planification d'un traitement.
 *
 * Construite par le Controller après extraction du JWT.
 * acteurId   : ID du pharmacien (extrait du token)
 * acteurRole : "PHARMACIEN" (extrait du token) — R1 vérifiée par le domaine
 */
public record PlanifierTraitementCommand(
        String             acteurId,
        String             acteurRole,
        String             patientUserId,
        LocalDate          dateDebut,
        LocalDate          dateFin,
        String             motif,
        List<LigneCommand> lignes
) {
    /**
     * Sous-commande pour chaque médicament prescrit.
     * heuresPrise : heures exactes fixées par le pharmacien — ex: [08:00, 14:00, 20:00]
     */
    public record LigneCommand(
            String          medicamentId,
            String          medicamentNom,
            String          principeActif,
            String          dosage,
            int             dureeJours,
            List<LocalTime> heuresPrise,
            String          instructions
    ) {}
}