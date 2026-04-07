package com.pharmaApp.user.application.service;

import com.pharmaApp.user.application.dto.request.LoginRequest;
import com.pharmaApp.user.application.dto.request.RegisterPatientRequest;
import com.pharmaApp.user.application.dto.response.AuthResponse;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.domain.model.PatientProfile;
import com.pharmaApp.user.domain.port.input.AuthUseCase;
import com.pharmaApp.user.domain.port.output.KeycloakUserCreationPort;
import com.pharmaApp.user.domain.port.output.PatientProfileRepositoryPort;
import com.pharmaApp.user.domain.port.output.QRCodeGeneratorPort; // ✅
import com.pharmaApp.user.application.mapper.PatientProfileMapper;
import com.pharmaApp.user.infrastructure.adapter.output.keycloak.KeycloakLoginClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final KeycloakUserCreationPort keycloakUserCreationPort;
    private final KeycloakLoginClient keycloakLoginClient;
    private final PatientProfileRepositoryPort patientProfileRepository;
    private final PatientProfileMapper patientProfileMapper;
    private final QRCodeGeneratorPort qrCodeGenerator; // ✅

    @Override
    public AuthResponse register(RegisterPatientRequest request) {
        log.info("📝 Register: {}", request.getEmail());

        // 1. Créer dans Keycloak
        String userId = keycloakUserCreationPort.createUser(
                request.getEmail(),
                request.getMotDePasse(),
                request.getNom(),
                request.getPrenom(),
                "PATIENT"
        );

        // 2. Créer profil en BDD — sans QR code
        PatientProfile profile = patientProfileMapper.toDomain(request);
        profile.setUserId(userId);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        // ✅ pas de QR code ici
        patientProfileRepository.save(profile);

        // 3. Login automatique → génère le QR code
        return buildAuthResponse(
                keycloakLoginClient.login(request.getEmail(), request.getMotDePasse()),
                userId, request.getNom(), request.getPrenom(), request.getEmail()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("🔐 Login: {}", request.getEmail());

        // 1. Login Keycloak
        AuthResponse auth = keycloakLoginClient.login(
                request.getEmail(), request.getMotDePasse()
        );

        // 2. Enrichir avec profil BDD + générer QR si absent
        try {
            patientProfileRepository.findByUserId(auth.getUserId())
                    .ifPresent(profile -> {
                        auth.setNom(profile.getNom());
                        auth.setPrenom(profile.getPrenom());
                        auth.setEmail(profile.getEmail());

                        // ✅ Générer QR code si absent
                        if (profile.getQrCode() == null
                                || profile.getQrCode().isBlank()) {
                            log.info("🔲 Génération QR code pour: {}", auth.getUserId());
                            String qrCode = qrCodeGenerator.generate(auth.getUserId());
                            profile.setQrCode(qrCode);
                            profile.setUpdatedAt(LocalDateTime.now());
                            PatientProfile saved = patientProfileRepository.save(profile);
                            auth.setQrCode(saved.getQrCode());
                        } else {
                            auth.setQrCode(profile.getQrCode()); // ✅ retourner existant
                        }
                    });
        } catch (Exception e) {
            log.warn("⚠️ Profil non trouvé pour: {}", auth.getUserId());
        }

        return auth;
    }

    @Override
    public PatientProfileResponse getMe(String authHeader) {
        String token  = authHeader.replace("Bearer ", "");
        String userId = keycloakLoginClient.extractUserId(token);

        PatientProfile profile = patientProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));

        return patientProfileMapper.toResponse(profile);
    }

    // ✅ Méthode utilitaire privée
    private AuthResponse buildAuthResponse(AuthResponse auth,
                                           String userId,
                                           String nom,
                                           String prenom,
                                           String email) {
        auth.setNom(nom);
        auth.setPrenom(prenom);
        auth.setEmail(email);

        // Générer QR code au premier login (juste après register)
        try {
            patientProfileRepository.findByUserId(userId)
                    .ifPresent(profile -> {
                        if (profile.getQrCode() == null
                                || profile.getQrCode().isBlank()) {
                            String qrCode = qrCodeGenerator.generate(userId);
                            profile.setQrCode(qrCode);
                            profile.setUpdatedAt(LocalDateTime.now());
                            patientProfileRepository.save(profile);
                            auth.setQrCode(qrCode);
                        } else {
                            auth.setQrCode(profile.getQrCode());
                        }
                    });
        } catch (Exception e) {
            log.warn("⚠️ QR code non généré: {}", e.getMessage());
        }

        return auth;
    }
}