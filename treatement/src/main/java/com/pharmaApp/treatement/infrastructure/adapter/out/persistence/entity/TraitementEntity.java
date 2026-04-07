package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA — table "traitement"
 * Aggregate Root persistence — correspond au domain model Traitement
 */
@Getter
@Setter
@Entity
@Table(
        name = "traitement",
        indexes = {
                @Index(name = "idx_traitement_patient_statut", columnList = "patient_user_id, statut"),
                @Index(name = "idx_traitement_pharmacien",     columnList = "pharmacien_user_id")
        }
)
public class TraitementEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "patient_user_id", length = 36, nullable = false)
    private String patientUserId;

    @Column(name = "pharmacien_user_id", length = 36, nullable = false)
    private String pharmacienUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    private TraitementStatutJpa statut;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "notes_pharmacien", columnDefinition = "TEXT")
    private String notesPharmacien;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Relations ────────────────────────────────────────────────
    @OneToMany(
            mappedBy = "traitement",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LigneMedicamentEntity> lignes = new ArrayList<>();

    @OneToMany(
            mappedBy = "traitement",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PrisePlanifieeEntity> prises = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        if (this.statut == null) this.statut = TraitementStatutJpa.ACTIF;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TraitementStatutJpa { ACTIF, TERMINE, SUSPENDU, ANNULE }
}