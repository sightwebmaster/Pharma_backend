package com.pharmaApp.user.application.service;

import com.pharmaApp.user.application.dto.request.AddProcheByEmailRequest;
import com.pharmaApp.user.application.dto.request.AddProcheByQrCodeRequest;
import com.pharmaApp.user.application.dto.request.CreatePatientProfileRequest;
import com.pharmaApp.user.application.dto.request.UpdatePatientProfileRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.application.mapper.PatientProfileMapper;
import com.pharmaApp.user.application.mapper.ProcheMapper;
import com.pharmaApp.user.domain.exception.ProfileAlreadyExistsException;
import com.pharmaApp.user.domain.exception.ProfileNotFoundException;
import com.pharmaApp.user.domain.exception.UnauthorizedAccessException;
import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.domain.model.Proche;
import com.pharmaApp.user.domain.port.input.PatientProfileUseCase;
import com.pharmaApp.user.domain.port.output.PatientProfileRepositoryPort;
import com.pharmaApp.user.domain.port.output.ProcheRepositoryPort;
import com.pharmaApp.user.domain.port.output.QRCodeGeneratorPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PatientProfileService implements PatientProfileUseCase {

    private final PatientProfileRepositoryPort patientProfileRepository;
    private final ProcheRepositoryPort procheRepository;
    private final QRCodeGeneratorPort qrCodeGenerator;
    private final PatientProfileMapper patientProfileMapper;
    private final ProcheMapper procheMapper;

    public PatientProfileService(
            PatientProfileRepositoryPort patientProfileRepository,
            ProcheRepositoryPort procheRepository,
            QRCodeGeneratorPort qrCodeGenerator,
            PatientProfileMapper patientProfileMapper,
            ProcheMapper procheMapper) {
        this.patientProfileRepository = patientProfileRepository;
        this.procheRepository = procheRepository;
        this.qrCodeGenerator = qrCodeGenerator;
        this.patientProfileMapper = patientProfileMapper;
        this.procheMapper = procheMapper;
    }

    // ─────────────────────────────────────────────
    // PROFIL PATIENT
    // ─────────────────────────────────────────────

    @Override
    public PatientProfileResponse createProfile(String userId,
                                                CreatePatientProfileRequest request) {
        if (patientProfileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException(
                    "Un profil existe déjà pour l'utilisateur : " + userId
            );
        }
        // 2. Convertir Request → Domain
        PatientProfile profile = patientProfileMapper.toDomain(request);
        profile.setUserId(userId);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        // 3. Générer le QR Code (contient le userId)
        String qrCode = qrCodeGenerator.generate(userId);
        profile.setQrCode(qrCode);

        // 4. Sauvegarder
        PatientProfile saved = patientProfileRepository.save(profile);

        // 5. Retourner la réponse
        return patientProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(String userId) {
        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil introuvable pour l'utilisateur : " + userId
                ));
        return patientProfileMapper.toResponse(profile);
    }

    @Override
    public PatientProfileResponse updateProfile(String userId,
                                                UpdatePatientProfileRequest request) {
        // 1. Récupérer le profil existant
        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil introuvable pour l'utilisateur : " + userId
                ));

        // 2. Appliquer uniquement les champs non-null (NullValuePropertyMappingStrategy.IGNORE)
        patientProfileMapper.updateDomainFromRequest(request, profile);
        profile.setUpdatedAt(LocalDateTime.now());

        // 3. Sauvegarder et retourner
        PatientProfile updated = patientProfileRepository.save(profile);
        return patientProfileMapper.toResponse(updated);
    }

    @Override
    public void deleteProfile(String userId) {
        if (!patientProfileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException(
                    "Profil introuvable pour l'utilisateur : " + userId
            );
        }
        patientProfileRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getQrCode(String userId) {
        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Profil introuvable pour l'utilisateur : " + userId
                ));

        // Régénérer si absent
        if (profile.getQrCode() == null || profile.getQrCode().isBlank()) {
            String qrCode = qrCodeGenerator.generate(userId);
            profile.setQrCode(qrCode);
            patientProfileRepository.save(profile);
            return qrCode;
        }

        return profile.getQrCode();
    }

    // ─────────────────────────────────────────────
// PROCHES
// ─────────────────────────────────────────────

    @Override
    public ProcheResponse addProcheByEmail(String userId, AddProcheByEmailRequest request) {
        if (!patientProfileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException("Patient introuvable : " + userId);
        }
        PatientProfile procheProfile = patientProfileRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Aucun patient avec l'email : " + request.getEmail()
                ));
        if (procheProfile.getUserId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez pas vous ajouter comme proche");
        }
        if (procheRepository.existsByPatientUserIdAndProcheUserId(
                userId, procheProfile.getUserId())) {
            throw new RuntimeException("Ce proche est déjà lié");
        }
        Proche proche = Proche.builder()
                .patientUserId(userId)
                .procheUserId(procheProfile.getUserId())
                .relation(request.getRelation())
                .build();
        return buildProcheResponse(procheRepository.save(proche), procheProfile);
    }

    @Override
    public ProcheResponse addProcheByQrCode(String userId, AddProcheByQrCodeRequest request) {
        if (!patientProfileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException("Patient introuvable : " + userId);
        }
        if (request.getProcheUserId().equals(userId)) {
            throw new RuntimeException("Vous ne pouvez pas vous ajouter comme proche");
        }
        PatientProfile procheProfile = patientProfileRepository
                .findByUserId(request.getProcheUserId())
                .orElseThrow(() -> new ProfileNotFoundException("Patient introuvable"));
        if (procheRepository.existsByPatientUserIdAndProcheUserId(
                userId, request.getProcheUserId())) {
            throw new RuntimeException("Ce proche est déjà lié");
        }
        Proche proche = Proche.builder()
                .patientUserId(userId)
                .procheUserId(request.getProcheUserId())
                .relation(request.getRelation())
                .build();
        return buildProcheResponse(procheRepository.save(proche), procheProfile);
    }

    @Override
    public List<ProcheResponse> getProches(String userId) {
        if (!patientProfileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException("Patient introuvable : " + userId);
        }
        return procheRepository.findAllByPatientUserId(userId)
                .stream()
                .map(proche -> {
                    PatientProfile procheProfile = patientProfileRepository
                            .findByUserId(proche.getProcheUserId())
                            .orElse(null);
                    return buildProcheResponse(proche, procheProfile);
                })
                .toList();
    }

    @Override
    public PatientProfileResponse getProcheProfile(String userId, String procheId) {
        Proche proche = procheRepository.findById(procheId)
                .orElseThrow(() -> new ProfileNotFoundException("Proche introuvable"));
        if (!proche.getPatientUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès non autorisé");
        }
        return patientProfileMapper.toResponse(
                patientProfileRepository.findByUserId(proche.getProcheUserId())
                        .orElseThrow(() -> new ProfileNotFoundException("Profil proche introuvable"))
        );
    }

    @Override
    public void deleteProche(String userId, String procheId) {
        if (!procheRepository.existsByIdAndPatientUserId(procheId, userId)) {
            throw new UnauthorizedAccessException("Ce proche n'appartient pas à ce patient");
        }
        procheRepository.deleteById(procheId);
    }

    // ✅ Méthode privée utilitaire
    private ProcheResponse buildProcheResponse(Proche proche, PatientProfile procheProfile) {
        ProcheResponse response = procheMapper.toResponse(proche);
        if (procheProfile != null) {
            response.setNom(procheProfile.getNom());
            response.setPrenom(procheProfile.getPrenom());
            response.setTelephone(procheProfile.getTelephone());
            response.setEmail(procheProfile.getEmail());
            response.setGroupeSanguin(procheProfile.getGroupeSanguin());
        }
        return response;
    }

}

