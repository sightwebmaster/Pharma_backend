
package com.pharmaApp.medication.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "medicaments",
        indexes = {
                @Index(name = "idx_med_nom",            columnList = "nom"),
                @Index(name = "idx_med_principe_actif", columnList = "principe_actif"),
                @Index(name = "idx_med_source",         columnList = "source")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MedicamentEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "principe_actif", nullable = false, length = 200)
    private String principeActif;

    @Column(length = 100)
    private String dosage;

    @Column(length = 100)
    private String forme; // comprimé, sirop, injection...

    @Column(length = 500)
    private String description;

    /** Symptômes traités — pour la recherche par symptômes */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptomes", columnDefinition = "json")
    private List<String> symptomes;

    /** Contre-indications médicales */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contre_indications", columnDefinition = "json")
    private List<String> contreIndications;

    /** Effets secondaires */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "effets_secondaires", columnDefinition = "json")
    private List<String> effetsSecondaires;

    /** Allergènes contenus (pénicilline, lactose, gluten...) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergenes", columnDefinition = "json")
    private List<String> allergenes;

    /** Restrictions par âge : "enfant_moins_12", "grossesse", "allaitement" */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "restrictions_age", columnDefinition = "json")
    private List<String> restrictionsAge;

    @Column(length = 10)
    private String prix;

    /** "interne" ou "openFDA" */
    @Column(length = 20, nullable = false)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}