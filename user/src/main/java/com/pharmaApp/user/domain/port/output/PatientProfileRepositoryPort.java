package com.pharmaApp.user.domain.port.output;

import com.pharmaApp.user.domain.model.PatientProfile;
import java.util.Optional;

public interface PatientProfileRepositoryPort {
    PatientProfile save(PatientProfile profile);
    Optional<PatientProfile> findByUserId(String userId);
    Optional<PatientProfile> findByEmail(String email); // ✅ nouveau
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}