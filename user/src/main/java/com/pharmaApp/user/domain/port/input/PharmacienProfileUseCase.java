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

    /**
     * UC — Scan QR Code patient
     * Le pharmacien scanne le QR du patient → lie les deux en DB.
     * Idempotent : si déjà lié → retourne le profil sans créer de doublon.
     *
     * @param pharmacienUserId ID Keycloak du pharmacien
     * @param patientUserId    ID Keycloak du patient (contenu du QR Code)
     * @return Profil du patient scanné
     */
    PatientProfileResponse scanQrCode(String pharmacienUserId, String patientUserId);

    /**
     * UC — Dashboard pharmacien
     * Retourne tous les patients liés à ce pharmacien.
     *
     * @param pharmacienUserId ID Keycloak du pharmacien
     * @return Liste des profils patients
     */
    @Transactional(readOnly = true)
    List<PatientProfileResponse> getMesPatients(String pharmacienUserId);
}