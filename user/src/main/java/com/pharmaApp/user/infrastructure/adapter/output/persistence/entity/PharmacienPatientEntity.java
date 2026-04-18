package com.pharmaApp.user.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Table de liaison pharmacien ↔ patient
 * Créée quand le pharmacien scanne le QR Code du patient.
 */
@Entity
@Table(
        name = "pharmacien_patients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pharmacien_patient",
                columnNames = {"pharmacien_user_id", "patient_user_id"}
        ),
        indexes = {
                @Index(name = "idx_pharmacien_patients", columnList = "pharmacien_user_id"),
                @Index(name = "idx_patient_pharmaciens", columnList = "patient_user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacienPatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "pharmacien_user_id", nullable = false)
    private String pharmacienUserId;

    @Column(name = "patient_user_id", nullable = false)
    private String patientUserId;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @PrePersist
    protected void onCreate() {
        this.dateAjout = LocalDateTime.now();
    }
}