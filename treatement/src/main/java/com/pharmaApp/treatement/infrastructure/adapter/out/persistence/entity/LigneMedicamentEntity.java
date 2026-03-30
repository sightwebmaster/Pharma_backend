package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA — table "ligne_medicament"
 * Une ligne = un médicament prescrit dans un traitement.
 * medicament_nom est dénormalisé (snapshot) : si le catalogue change,
 * l'historique de prescription reste intact.
 */
@Getter
@Setter
@Entity
@Table(
        name = "ligne_medicament",
        indexes = {
                @Index(name = "idx_ligne_traitement_id", columnList = "traitement_id")
        }
)
public class LigneMedicamentEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Relation N..1 vers Traitement.
     * FetchType.LAZY : on ne charge pas le traitement entier si on interroge
     * uniquement les lignes.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "traitement_id", nullable = false)
    private TraitementEntity traitement;

    /**
     * Référence vers medication-service — pas de FK inter-services
     */
    @Column(name = "medicament_id", length = 36, nullable = false)
    private String medicamentId;

    /**
     * Snapshot du nom au moment de la prescription
     */
    @Column(name = "medicament_nom", length = 200, nullable = false)
    private String medicamentNom;

    @Column(name = "principe_actif", length = 200)
    private String principeActif;

    @Column(name = "dosage", length = 100, nullable = false)
    private String dosage;

    @Column(name = "frequence_par_jour", nullable = false)
    private Integer frequenceParJour;

    @Column(name = "duree_jours", nullable = false)
    private Integer dureeJours;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Relation 1..N vers les prises planifiées générées pour cette ligne
     */
    @OneToMany(
            mappedBy = "ligneMedicament",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PrisePlanifieeEntity> prises = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
}