package com.pharmaApp.user.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienPatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacienPatientJpaRepository
        extends JpaRepository<PharmacienPatientEntity, String> {

    /** Tous les patients d'un pharmacien */
    List<PharmacienPatientEntity> findByPharmacienUserId(String pharmacienUserId);

    /** Tous les pharmaciens d'un patient */
    List<PharmacienPatientEntity> findByPatientUserId(String patientUserId);

    /** Vérifie si la relation existe déjà */
    boolean existsByPharmacienUserIdAndPatientUserId(
            String pharmacienUserId, String patientUserId);
}