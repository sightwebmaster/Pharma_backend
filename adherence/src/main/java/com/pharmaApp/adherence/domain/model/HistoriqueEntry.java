package com.pharmaApp.adherence.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Value Object — EntreeHistorique
 *
 * Représente une prise unique (confirmée ou manquée).
 * Immuable : on n'édite jamais une prise passée.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueEntry {

    private Long id;

    /** FK vers AdherenceRecord */
    private Long adherenceRecordId;

    /** ID de la prise dans treatment-service */
    private Long priseMedicamentId;

    /** Nom du médicament (dénormalisé pour éviter appel inter-service à chaque lecture) */
    private String medicamentNom;

    /** Dosage (ex: "500mg") */
    private String dosage;

    /** Date à laquelle la prise était attendue */
    private LocalDate datePrise;

    /** Heure à laquelle la prise était attendue */
    private LocalTime heurePrise;

    /** CONFIRME ou MANQUE */
    private StatutPrise statut;

    /** Heure réelle de confirmation (null si MANQUE) */
    private LocalTime heureConfirmation;

    /** Délai en minutes entre heure prévue et heure réelle (null si MANQUE) */
    private Integer delaiMinutes;

    /** Note optionnelle du patient */
    private String notePatient;
}
