package com.pharmaApp.user.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // ✅ UUID comme Keycloak
    private String id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;


    private String telephone;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "groupe_sanguin", length = 5)
    private String groupeSanguin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> allergies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "maladies_chroniques", columnDefinition = "json")
    private List<String> maladiesChroniques;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ PAS de toDomain() ni fromDomain() ici
    // C'est PatientProfileEntityMapper (MapStruct) qui fait ce travail
}