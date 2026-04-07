package com.pharmaApp.user.infrastructure.adapter.input.rest;

import com.pharmaApp.user.application.dto.request.CreatePatientByPharmacienRequest;
import com.pharmaApp.user.application.dto.request.ScanQrCodeRequest;
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

    // ── Profil pharmacien ────────────────────────────────────────

    @GetMapping("/{userId}/profile")
    public ResponseEntity<PharmacienProfileResponse> getMyProfile(
            @PathVariable String userId) {
        log.info("Consultation profil pharmacien: {}", userId);
        return ResponseEntity.ok(pharmacienProfileUseCase.getMyProfile(userId));
    }

    // ── Créer patient ────────────────────────────────────────────

    @PostMapping("/{pharmacienUserId}/create-patient")
    public ResponseEntity<PatientProfileResponse> createPatient(
            @PathVariable String pharmacienUserId,
            @Valid @RequestBody CreatePatientByPharmacienRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pharmacienProfileUseCase.createPatient(pharmacienUserId, request));
    }

    // ── Scan QR Code ─────────────────────────────────────────────

    /**
     * POST /api/v1/pharmaciens/{pharmacienId}/scan-qr
     * Le pharmacien scanne le QR Code d'un patient.
     * Body : { "patientUserId": "uuid-keycloak-du-patient" }
     * Retourne le profil du patient + le lie au pharmacien en DB.
     */
    @PostMapping("/{pharmacienUserId}/scan-qr")
    public ResponseEntity<PatientProfileResponse> scanQrCode(
            @PathVariable String pharmacienUserId,
            @Valid @RequestBody ScanQrCodeRequest request) {

        log.info("Scan QR — pharmacien={} patient={}",
                pharmacienUserId, request.getPatientUserId());

        PatientProfileResponse response =
                pharmacienProfileUseCase.scanQrCode(
                        pharmacienUserId, request.getPatientUserId());

        return ResponseEntity.ok(response);
    }

    // ── Dashboard — liste des patients ───────────────────────────

    /**
     * GET /api/v1/pharmaciens/{pharmacienId}/mes-patients
     * Retourne tous les patients liés à ce pharmacien.
     * Utilisé pour charger le dashboard pharmacien.
     */
    @GetMapping("/{pharmacienUserId}/mes-patients")
    public ResponseEntity<List<PatientProfileResponse>> getMesPatients(
            @PathVariable String pharmacienUserId) {

        log.info("Dashboard pharmacien={}", pharmacienUserId);

        return ResponseEntity.ok(
                pharmacienProfileUseCase.getMesPatients(pharmacienUserId));
    }

    // ── Consultation patient ─────────────────────────────────────

    @GetMapping("/patients/{patientUserId}/profile")
    public ResponseEntity<PatientProfileResponse> getPatientProfile(
            @RequestParam String pharmacienUserId,
            @PathVariable String patientUserId) {
        return ResponseEntity.ok(
                pharmacienProfileUseCase.getPatientProfile(
                        pharmacienUserId, patientUserId));
    }

    @GetMapping("/patients/{patientUserId}/proches")
    public ResponseEntity<List<ProcheResponse>> getPatientProches(
            @RequestParam String pharmacienUserId,
            @PathVariable String patientUserId) {
        return ResponseEntity.ok(
                pharmacienProfileUseCase.getPatientProches(
                        pharmacienUserId, patientUserId));
    }
}