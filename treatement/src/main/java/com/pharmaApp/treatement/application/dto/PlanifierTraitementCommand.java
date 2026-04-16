package com.pharmaApp.treatement.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import com.pharmaApp.treatement.infrastructure.adapter.out.client.MedicationRestClient;

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
        List<LigneMedicament> lignes
) {
    /**
     * Sous-commande pour chaque médicament prescrit.
     * heuresPrise : heures exactes fixées par le pharmacien — ex: [08:00, 14:00, 20:00]
     */
    public record LigneMedicament(
            String          id,
            String          medicamentId,
            String          medicamentNom,
            String          principeActif,
            String          dosage,
            int             dureeJours,
            List<LocalTime> heuresPrise,
            String          instructions
    ) {
        // Constructeur qui génère automatiquement un UUID si id est null
        public LigneMedicament {
            if (id == null || id.isBlank()) {
                id = UUID.randomUUID().toString();
            }
        }

        // Factory method pour créer une ligne avec ID généré automatiquement
        public static LigneMedicament create(
                String medicamentId,
                String medicamentNom,
                String principeActif,
                String dosage,
                int dureeJours,
                List<LocalTime> heuresPrise,
                String instructions
        ) {
            return new LigneMedicament(
                    UUID.randomUUID().toString(),
                    medicamentId,
                    medicamentNom,
                    principeActif,
                    dosage,
                    dureeJours,
                    heuresPrise,
                    instructions
            );
        }
    }
}