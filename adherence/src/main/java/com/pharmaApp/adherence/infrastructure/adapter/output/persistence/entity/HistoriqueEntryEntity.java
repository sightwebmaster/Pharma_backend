package com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
    name = "historique_entries",
    indexes = {
        @Index(name = "idx_entry_date",   columnList = "date_prise"),
        @Index(name = "idx_entry_statut", columnList = "statut")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adherence_record_id", nullable = false)
    private AdherenceRecordEntity adherenceRecord;

    @Column(name = "prise_medicament_id", nullable = false)
    private Long priseMedicamentId;

    @Column(name = "medicament_nom", nullable = false, length = 200)
    private String medicamentNom;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "date_prise", nullable = false)
    private LocalDate datePrise;

    @Column(name = "heure_prise", nullable = false)
    private LocalTime heurePrise;

    /**
     * CONFIRME | MANQUE
     * Stocké en String pour lisibilité directe dans la BDD.
     */
    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "heure_confirmation")
    private LocalTime heureConfirmation;

    /** Délai en minutes entre heure prévue et heure réelle */
    @Column(name = "delai_minutes")
    private Integer delaiMinutes;

    @Column(name = "note_patient", length = 500)
    private String notePatient;
}
