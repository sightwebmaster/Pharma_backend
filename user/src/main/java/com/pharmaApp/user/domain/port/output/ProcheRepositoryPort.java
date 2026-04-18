package com.pharmaApp.user.domain.port.output;

import com.pharmaApp.user.domain.model.Proche;
import java.util.List;
import java.util.Optional;

public interface ProcheRepositoryPort {
    Proche save(Proche proche);
    Optional<Proche> findById(String id);
    List<Proche> findAllByPatientUserId(String patientUserId);
    List<Proche> findAllByProcheUserId(String procheUserId);
    boolean existsByIdAndPatientUserId(String id, String patientUserId);
    boolean existsByPatientUserIdAndProcheUserId(String patientUserId, String procheUserId); // ✅
    void deleteById(String id);
}
