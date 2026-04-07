    package com.pharmaApp.user.infrastructure.adapter.output.persistence.entity;

    import jakarta.persistence.*;
    import lombok.*;
    import java.time.LocalDateTime;

    @Entity
    @Table(name = "pharmacien_profiles")
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public class PharmacienProfileEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        @Column(name = "user_id", unique = true, nullable = false)
        private String userId;

        @Column(nullable = false)
        private String nom;

        @Column(nullable = false)
        private String prenom;

        @Column(unique = true)
        private String email;

        private String telephone;

        @Column(name = "numero_ordre", unique = true)
        private String numeroOrdre;

        private String specialite;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;
    }

