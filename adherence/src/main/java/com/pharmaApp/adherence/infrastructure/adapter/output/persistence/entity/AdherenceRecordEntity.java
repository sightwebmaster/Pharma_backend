package com.pharmaApp.adherence.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "adherence_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_patient_traitement",
        columnNames = {"patient_user_id", "traitement_id"}
    ),
    indexes = {
        @Index(name = "idx_patient",    columnList = "patient_user_id"),
        @Index(name = "idx_pharmacien", columnList = "pharmacien_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdherenceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_user_id", nullable = false, length = 100)
    private String patientUserId;

    @Column(name = "traitement_id", nullable = false)
    private Long traitementId;

    @Column(name = "pharmacien_user_id", nullable = false, length = 100)
    private String pharmacienUserId;

    // ── Taux d'observance ──────────────────────────────────────

    @Column(name = "taux_7j", nullable = false)
    private double taux7j = 100.0;

    @Column(name = "taux_30j", nullable = false)
    private double taux30j = 100.0;

    @Column(name = "taux_90j", nullable = false)
    private double taux90j = 100.0;

    @Column(name = "taux_global", nullable = false)
    private double tauxGlobal = 100.0;

    @Column(name = "consecutive_missed", nullable = false)
    private int consecutiveMissed = 0;

    @UpdateTimestamp
    @Column(name = "last_calculated")
    private LocalDate lastCalculated;

    // ── Relation ────────────────────────────────────────────────

    @OneToMany(
        mappedBy = "adherenceRecord",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("datePrise ASC, heurePrise ASC")
    @Builder.Default
    private List<HistoriqueEntryEntity> entries = new ArrayList<>();
}
