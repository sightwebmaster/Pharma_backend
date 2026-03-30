package com.pharmaApp.user.domain.port.output;

import com.pharmaApp.user.domain.model.PharmacienProfile;
import java.util.Optional;

public interface PharmacienProfileRepositoryPort {

    PharmacienProfile save(PharmacienProfile pharmacienProfile);

    Optional<PharmacienProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    void deleteByUserId(String userId);                               // ✅ présent
}