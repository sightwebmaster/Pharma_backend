package com.pharmaApp.user.application.service;

import com.pharmaApp.user.application.dto.request.CreatePatientByPharmacienRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.application.dto.response.PharmacienProfileResponse;
import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.application.mapper.PatientProfileMapper;
import com.pharmaApp.user.application.mapper.PharmacienProfileMapper;
import com.pharmaApp.user.application.mapper.ProcheMapper;
import com.pharmaApp.user.domain.exception.ProfileNotFoundException;
import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.domain.model.Proche;
import com.pharmaApp.user.domain.port.input.PharmacienProfileUseCase;
import com.pharmaApp.user.domain.port.output.PatientProfileRepositoryPort;
import com.pharmaApp.user.domain.port.output.PharmacienProfileRepositoryPort;
import com.pharmaApp.user.domain.port.output.ProcheRepositoryPort;
import com.pharmaApp.user.domain.port.output.QRCodeGeneratorPort;
import com.pharmaApp.user.domain.port.output.KeycloakUserCreationPort;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.entity.PharmacienPatientEntity;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.repository.PharmacienPatientJpaRepository;
import com.pharmaApp.user.infrastructure.adapter.output.persistence.repository.PatientProfileJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PharmacienProfileService implements PharmacienProfileUseCase {

    private final PharmacienProfileRepositoryPort pharmacienRepository;
    private final PatientProfileRepositoryPort    patientRepository;
    private final ProcheRepositoryPort            procheRepository;
    private final KeycloakUserCreationPort        keycloakUserCreationPort;
    private final QRCodeGeneratorPort             qrCodeGenerator;
    private final PharmacienProfileMapper         pharmacienMapper;
    private final PatientProfileMapper            patientMapper;
    private final ProcheMapper                    procheMapper;

    // ── Nouveaux repositories pour la liaison pharmacien-patient ──
    private final PharmacienPatientJpaRepository  pharmacienPatientRepository;
    private final PatientProfileJpaRepository     patientJpaRepository;

    public PharmacienProfileService(
            PharmacienProfileRepositoryPort pharmacienRepository,
            PatientProfileRepositoryPort    patientRepository,
            ProcheRepositoryPort            procheRepository,
            KeycloakUserCreationPort        keycloakUserCreationPort,
            QRCodeGeneratorPort             qrCodeGenerator,
            PharmacienProfileMapper         pharmacienMapper,
            PatientProfileMapper            patientMapper,
            ProcheMapper                    procheMapper,
            PharmacienPatientJpaRepository  pharmacienPatientRepository,
            PatientProfileJpaRepository     patientJpaRepository) {

        this.pharmacienRepository        = pharmacienRepository;
        this.patientRepository           = patientRepository;
        this.procheRepository            = procheRepository;
        this.keycloakUserCreationPort    = keycloakUserCreationPort;
        this.qrCodeGenerator             = qrCodeGenerator;
        this.pharmacienMapper            = pharmacienMapper;
        this.patientMapper               = patientMapper;
        this.procheMapper                = procheMapper;
        this.pharmacienPatientRepository = pharmacienPatientRepository;
        this.patientJpaRepository        = patientJpaRepository;
    }

    // ─────────────────────────────────────────────
    // PROFIL PHARMACIEN
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PharmacienProfileResponse getMyProfile(String userId) {
        return pharmacienRepository
                .findByUserId(userId)
                .map(pharmacienMapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil pharmacien introuvable pour : " + userId));
    }

    // ─────────────────────────────────────────────
    // ACCÈS AUX PATIENTS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientProfile(String pharmacienUserId,
                                                    String patientUserId) {
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId);
        }
        return patientRepository
                .findByUserId(patientUserId)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil patient introuvable : " + patientUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcheResponse> getPatientProches(String pharmacienUserId,
                                                  String patientUserId) {
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId);
        }
        if (!patientRepository.existsByUserId(patientUserId)) {
            throw new ProfileNotFoundException(
                    "Profil patient introuvable : " + patientUserId);
        }
        List<Proche> proches = procheRepository.findAllByPatientUserId(patientUserId);
        return procheMapper.toResponseList(proches);
    }

    // ─────────────────────────────────────────────
    // CRÉER UN PATIENT
    // ─────────────────────────────────────────────

    @Override
    public PatientProfileResponse createPatient(String pharmacienUserId,
                                                CreatePatientByPharmacienRequest request) {
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId);
        }

        String newUserId = keycloakUserCreationPort.createUser(
                request.getEmail(),
                request.getMotDePasse(),
                request.getNom(),
                request.getPrenom(),
                "PATIENT"
        );

        PatientProfile profile = new PatientProfile();
        profile.setUserId(newUserId);
        profile.setNom(request.getNom());
        profile.setPrenom(request.getPrenom());
        profile.setDateNaissance(request.getDateNaissance());
        profile.setTelephone(request.getTelephone());
        profile.setGroupeSanguin(request.getGroupeSanguin());
        profile.setAllergies(request.getAllergies());
        profile.setMaladiesChroniques(request.getMaladiesChroniques());
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setQrCode(qrCodeGenerator.generate(newUserId));

        PatientProfile saved = patientRepository.save(profile);

        // ✅ Lie automatiquement le patient créé à ce pharmacien
        lierPharmacienPatient(pharmacienUserId, newUserId);

        return patientMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // SCAN QR CODE
    // ─────────────────────────────────────────────

    @Override
    public PatientProfileResponse scanQrCode(String pharmacienUserId,
                                             String patientUserId) {
        // 1. Vérifier que le pharmacien existe
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId);
        }

        // 2. Vérifier que le patient existe
        PatientProfile patient = patientRepository
                .findByUserId(patientUserId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil patient introuvable pour QR : " + patientUserId));

        // 3. Lier pharmacien ↔ patient (idempotent)
        lierPharmacienPatient(pharmacienUserId, patientUserId);

        log.info("QR scanné — pharmacien={} patient={}", pharmacienUserId, patientUserId);

        // 4. Retourner le profil du patient
        return patientMapper.toResponse(patient);
    }

    // ─────────────────────────────────────────────
    // DASHBOARD — MES PATIENTS
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PatientProfileResponse> getMesPatients(String pharmacienUserId) {
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId);
        }

        // 1. Récupérer tous les patientUserIds liés à ce pharmacien
        List<String> patientUserIds = pharmacienPatientRepository
                .findByPharmacienUserId(pharmacienUserId)
                .stream()
                .map(PharmacienPatientEntity::getPatientUserId)
                .collect(Collectors.toList());

        if (patientUserIds.isEmpty()) return List.of();

        // 2. Charger les profils patients via JPA directement
        return patientJpaRepository
                .findByUserIdIn(patientUserIds)
                .stream()
                .map(entity -> patientRepository
                        .findByUserId(entity.getUserId())
                        .map(patientMapper::toResponse)
                        .orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UTILITAIRE PRIVÉ
    // ─────────────────────────────────────────────

    /**
     * Crée la liaison pharmacien ↔ patient si elle n'existe pas déjà.
     * Idempotent — pas d'exception si déjà lié.
     */
    private void lierPharmacienPatient(String pharmacienUserId, String patientUserId) {
        boolean dejaLie = pharmacienPatientRepository
                .existsByPharmacienUserIdAndPatientUserId(pharmacienUserId, patientUserId);

        if (!dejaLie) {
            PharmacienPatientEntity liaison = PharmacienPatientEntity.builder()
                    .pharmacienUserId(pharmacienUserId)
                    .patientUserId(patientUserId)
                    .build();
            pharmacienPatientRepository.save(liaison);
            log.info("Liaison créée — pharmacien={} patient={}", pharmacienUserId, patientUserId);
        } else {
            log.debug("Liaison déjà existante — pharmacien={} patient={}", pharmacienUserId, patientUserId);
        }
    }
}