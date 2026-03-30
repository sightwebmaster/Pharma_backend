package com.pharmaApp.user.infrastructure.adapter.input.rest;

import com.pharmaApp.user.application.dto.request.CreatePatientByPharmacienRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.application.dto.response.PharmacienProfileResponse;
import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.domain.port.input.PharmacienProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/pharmaciens")
@RequiredArgsConstructor
public class PharmacienController {

    private final PharmacienProfileUseCase pharmacienProfileUseCase;

    // ─────────────────────────────────────────────────────────────
    // CRÉATION D'UN PATIENT PAR LE PHARMACIEN
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/pharmaciens/create-patient
     * Pharmacien crée un compte patient complet
     * (crée dans Keycloak + profil en BDD)
     *
     * @param request Les données du patient à créer (contient l'ID du pharmacien)
     * @return Le profil du patient créé
     */
    @PostMapping("/{pharmacienUserId}/create-patient")
    public ResponseEntity<PatientProfileResponse> createPatient(
            @PathVariable String pharmacienUserId,
            @Valid @RequestBody CreatePatientByPharmacienRequest request) {

        PatientProfileResponse response =
                pharmacienProfileUseCase.createPatient(pharmacienUserId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────
    // CONSULTATION DU PROFIL PHARMACIEN
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/pharmaciens/{userId}/profile
     * Pharmacien voit son propre profil
     *
     * @param userId L'ID du pharmacien dans Keycloak
     * @return Le profil du pharmacien
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<PharmacienProfileResponse> getMyProfile(
            @PathVariable String userId) {

        log.info("Consultation du profil du pharmacien: {}", userId);

        PharmacienProfileResponse response =
                pharmacienProfileUseCase.getMyProfile(userId);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // CONSULTATION DES PATIENTS
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/pharmaciens/patients/{patientUserId}/profile
     * Pharmacien voit le profil d'un patient
     *
     * @param pharmacienUserId L'ID du pharmacien (dans la requête)
     * @param patientUserId L'ID du patient à consulter
     * @return Le profil du patient
     */
    @GetMapping("/patients/{patientUserId}/profile")
    public ResponseEntity<PatientProfileResponse> getPatientProfile(
            @RequestParam String pharmacienUserId,
            @PathVariable String patientUserId) {

        log.info("Pharmacien {} consulte le profil du patient {}",
                pharmacienUserId, patientUserId);

        PatientProfileResponse response =
                pharmacienProfileUseCase.getPatientProfile(
                        pharmacienUserId, patientUserId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/pharmaciens/patients/{patientUserId}/proches
     * Pharmacien voit les proches d'un patient
     *
     * @param pharmacienUserId L'ID du pharmacien (dans la requête)
     * @param patientUserId L'ID du patient
     * @return La liste des proches
     */
    @GetMapping("/patients/{patientUserId}/proches")
    public ResponseEntity<List<ProcheResponse>> getPatientProches(
            @RequestParam String pharmacienUserId,
            @PathVariable String patientUserId) {

        log.info("Pharmacien {} consulte les proches du patient {}",
                pharmacienUserId, patientUserId);

        List<ProcheResponse> proches =
                pharmacienProfileUseCase.getPatientProches(
                        pharmacienUserId, patientUserId
                );

        return ResponseEntity.ok(proches);
    }
}