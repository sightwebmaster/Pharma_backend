package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ligne_medicament")
// ✅ Pas de @IdClass — clé simple
public class LigneMedicamentEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "traitement_id", nullable = false)
    private TraitementEntity traitement;

    @Column(name = "medicament_id", length = 36, nullable = true)
    private String medicamentId;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ligne_heures_prise",
            joinColumns = @JoinColumn(name = "ligne_id")  // ✅ 1 seule FK
    )
    @Column(name = "heure_prise")
    private List<LocalTime> heuresPrise = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }
}