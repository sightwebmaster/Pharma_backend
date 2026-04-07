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

    // ✅ String UUID — aligné avec treatment-service
    @Column(name = "prise_medicament_id", nullable = false, length = 36)
    private String priseMedicamentId;

    @Column(name = "medicament_nom", nullable = false, length = 200)
    private String medicamentNom;

    @Column(name = "dosage", length = 50)
    private String dosage;

    @Column(name = "date_prise", nullable = false)
    private LocalDate datePrise;

    @Column(name = "heure_prise")
    private LocalTime heurePrise;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "heure_confirmation")
    private LocalTime heureConfirmation;

    @Column(name = "delai_minutes")
    private Integer delaiMinutes;

    @Column(name = "note_patient", length = 500)
    private String notePatient;
}