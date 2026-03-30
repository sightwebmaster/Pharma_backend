package com.pharmaApp.user.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "patient_user_id", nullable = false)
    private String patientUserId;

    @Column(name = "proche_user_id", nullable = false)
    private String procheUserId;

    @Column(nullable = false)
    private String relation;
}