package com.pharmaApp.user.infrastructure.adapter.output.persistence.repository;

import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.ProcheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcheJpaRepository
        extends JpaRepository<ProcheEntity, String> {

    List<ProcheEntity> findByPatientUserId(String patientUserId);
    boolean existsByIdAndPatientUserId(String id, String patientUserId);
    boolean existsByPatientUserIdAndProcheUserId(String patientUserId, String procheUserId); // ✅
}