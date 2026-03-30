package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA — table "prise_planifiee"
 *
 * Générée automatiquement lors de la création d'un traitement :
 * pour chaque LigneMedicament, on génère (dureeJours × frequenceParJour) prises.
 *
 * patient_user_id est dénormalisé ici pour que le Scheduler puisse faire
 * une seule requête sans jointure :
 *   SELECT * FROM prise_planifiee
 *   WHERE statut = 'PLANIFIEE' AND heure_prevue <= :deadline
 */
@Getter
@Setter
@Entity
@Table(
        name = "prise_planifiee",
        indexes = {
                @Index(name = "idx_prise_statut_heure",
                        columnList = "statut, heure_prevue"),
                @Index(name = "idx_prise_patient_statut",
                        columnList = "patient_user_id, statut")
        }
)
public class PrisePlanifieeEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treaitement_id", nullable = false)
    private TraitementEntity traitement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_medicament_id", nullable = false)
    private LigneMedicamentEntity ligneMedicament;

    /**
     * Dénormalisé pour performance scheduler
     */
    @Column(name = "patient_user_id", length = 36, nullable = false)
    private String patientUserId;

    @Column(name = "heure_prevue", nullable = false)
    private LocalDateTime heurePrevue;

    /**
     * Null tant que non confirmée
     */
    @Column(name = "heure_reelle")
    private LocalDateTime heureReelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private PriseStatutJpa statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_confirmation", length = 20)
    private SourceConfirmationJpa sourceConfirmation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = PriseStatutJpa.PLANIFIEE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum PriseStatutJpa {PLANIFIEE, CONFIRMEE, MANQUEE}

    public enum SourceConfirmationJpa {PATIENT, SYSTEME}
}