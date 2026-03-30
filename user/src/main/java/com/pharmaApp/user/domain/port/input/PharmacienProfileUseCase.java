// PharmacienProfileUseCase.java
package com.pharmaApp.user.domain.port.input;

import com.pharmaApp.user.application.dto.request.*;
import com.pharmaApp.user.application.dto.response.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PharmacienProfileUseCase {

    @Transactional(readOnly = true)
    PharmacienProfileResponse getMyProfile(String userId);

    @Transactional(readOnly = true)
    PatientProfileResponse getPatientProfile(String pharmacienUserId,
                                             String patientUserId);

    @Transactional(readOnly = true)
    List<ProcheResponse> getPatientProches(String pharmacienUserId,
                                           String patientUserId);

    PatientProfileResponse createPatient(String pharmacienUserId,
                                         CreatePatientByPharmacienRequest request);
}