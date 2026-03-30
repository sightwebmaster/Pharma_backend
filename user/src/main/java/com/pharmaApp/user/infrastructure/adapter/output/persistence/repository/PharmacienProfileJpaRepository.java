package com.pharmaApp.user.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PharmacienProfileJpaRepository
        extends JpaRepository<PharmacienProfileEntity, String> {  // ✅ String pas String

    Optional<PharmacienProfileEntity> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);

    Optional<PharmacienProfileEntity> findByNumeroOrdre(String numeroOrdre);

    boolean existsByNumeroOrdre(String numeroOrdre);

}