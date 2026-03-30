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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PharmacienProfileService implements PharmacienProfileUseCase {

    private final PharmacienProfileRepositoryPort pharmacienRepository;
    private final PatientProfileRepositoryPort patientRepository;
    private final ProcheRepositoryPort procheRepository;
    private final KeycloakUserCreationPort keycloakUserCreationPort;
    private final QRCodeGeneratorPort qrCodeGenerator;
    private final PharmacienProfileMapper pharmacienMapper;
    private final PatientProfileMapper patientMapper;
    private final ProcheMapper procheMapper;

    public PharmacienProfileService(
            PharmacienProfileRepositoryPort pharmacienRepository,
            PatientProfileRepositoryPort patientRepository,
            ProcheRepositoryPort procheRepository,
            KeycloakUserCreationPort keycloakUserCreationPort,
            QRCodeGeneratorPort qrCodeGenerator,
            PharmacienProfileMapper pharmacienMapper,
            PatientProfileMapper patientMapper,
            ProcheMapper procheMapper) {
        this.pharmacienRepository = pharmacienRepository;
        this.patientRepository = patientRepository;
        this.procheRepository = procheRepository;
        this.keycloakUserCreationPort = keycloakUserCreationPort;
        this.qrCodeGenerator = qrCodeGenerator;
        this.pharmacienMapper = pharmacienMapper;
        this.patientMapper = patientMapper;
        this.procheMapper = procheMapper;
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
                        "Profil pharmacien introuvable pour : " + userId
                ));
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
                    "Profil pharmacien introuvable : " + pharmacienUserId
            );
        }
        return patientRepository
                .findByUserId(patientUserId)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil patient introuvable : " + patientUserId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcheResponse> getPatientProches(String pharmacienUserId,
                                                  String patientUserId) {
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId
            );
        }
        if (!patientRepository.existsByUserId(patientUserId)) {
            throw new ProfileNotFoundException(
                    "Profil patient introuvable : " + patientUserId
            );
        }
        List<Proche> proches = procheRepository.findAllByPatientUserId(patientUserId);
        return procheMapper.toResponseList(proches);
    }

    // ─────────────────────────────────────────────
    // CRÉER UN PATIENT (Pharmacien → Keycloak Admin API)
    // ─────────────────────────────────────────────

    @Override
    public PatientProfileResponse createPatient(String pharmacienUserId,
                                                CreatePatientByPharmacienRequest request) {
        // 1. Vérifier que le pharmacien existe
        if (!pharmacienRepository.existsByUserId(pharmacienUserId)) {
            throw new ProfileNotFoundException(
                    "Profil pharmacien introuvable : " + pharmacienUserId
            );
        }

        // 2. Créer le user dans Keycloak → retourne le Keycloak userId (UUID)
        String newUserId = keycloakUserCreationPort.createUser(
                request.getEmail(),
                request.getMotDePasse(),
                request.getNom(),
                request.getPrenom(),
                "PATIENT"
        );

        // 3. Créer le profil médical du patient
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

        // 4. Générer le QR Code
        profile.setQrCode(qrCodeGenerator.generate(newUserId));

        // 5. Sauvegarder et retourner
        PatientProfile saved = patientRepository.save(profile);
        return patientMapper.toResponse(saved);
    }
}