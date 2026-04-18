package com.pharmaApp.user.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PatientProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientProfileJpaRepository
        extends JpaRepository<PatientProfileEntity, String> {

    Optional<PatientProfileEntity> findByUserId(String userId);
    Optional<PatientProfileEntity> findByEmail(String email); // ✅
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
    List<PatientProfileEntity> findByUserIdIn(List<String> userIds);
}