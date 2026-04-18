package com.pharmaApp.user.infrastructure.adapter.input.rest;

import com.pharmaApp.user.application.dto.request.AddProcheByEmailRequest;
import com.pharmaApp.user.application.dto.request.AddProcheByQrCodeRequest;
import com.pharmaApp.user.application.dto.request.CreatePatientProfileRequest;
import com.pharmaApp.user.application.dto.request.UpdatePatientProfileRequest;
import com.pharmaApp.user.application.dto.response.PatientProfileResponse;
import com.pharmaApp.user.application.dto.response.ProcheResponse;
import com.pharmaApp.user.application.dto.response.RecommendationPatientContextResponse;
import com.pharmaApp.user.domain.port.input.PatientProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientProfileUseCase patientProfileUseCase;

    @Value("${internal.api.token}")
    private String internalApiToken;

    // ─────────────────────────────────────────────────────────────
    // PROFIL PATIENT
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/patients/{userId}/profile
     * Patient crée son propre profil médical
     */
    @PostMapping("/{userId}/profile")
    public ResponseEntity<PatientProfileResponse> createProfile(
            @PathVariable String userId,
            @Valid @RequestBody CreatePatientProfileRequest request) {

        // ✅ pas de @AuthenticationPrincipal Jwt jwt
        // ✅ pas de vérification jwt.getSubject()
        PatientProfileResponse response =
                patientProfileUseCase.createProfile(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<PatientProfileResponse> getProfile(
            @PathVariable String userId) {

        PatientProfileResponse response =
                patientProfileUseCase.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/recommendation-context")
    public ResponseEntity<RecommendationPatientContextResponse> getRecommendationContext(
            @PathVariable String userId,
            @RequestHeader(value = "X-Internal-Token", required = false) String providedToken) {

        if (providedToken == null || !providedToken.equals(internalApiToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(buildRecommendationContext(userId));
    }

    @GetMapping("/{patientId}/profile-for-reco")
    public ResponseEntity<RecommendationPatientContextResponse> getProfileForRecommendation(
            @PathVariable String patientId,
            @RequestHeader(value = "X-User-Id", required = false) String requesterUserId) {

        if (requesterUserId == null || !requesterUserId.equals(patientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(buildRecommendationContext(patientId));
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<PatientProfileResponse> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePatientProfileRequest request) {

        PatientProfileResponse response =
                patientProfileUseCase.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/profile")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        patientProfileUseCase.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/qrcode")
    public ResponseEntity<String> getQrCode(@PathVariable String userId) {
        String qrCode = patientProfileUseCase.getQrCode(userId);
        return ResponseEntity.ok(qrCode);
    }

    @PostMapping("/{userId}/proches/by-email")
    public ResponseEntity<ProcheResponse> addProcheByEmail(
            @PathVariable String userId,
            @Valid @RequestBody AddProcheByEmailRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientProfileUseCase.addProcheByEmail(userId, request));
    }

    @PostMapping("/{userId}/proches/by-qrcode")
    public ResponseEntity<ProcheResponse> addProcheByQrCode(
            @PathVariable String userId,
            @Valid @RequestBody AddProcheByQrCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientProfileUseCase.addProcheByQrCode(userId, request));
    }

    @GetMapping("/{userId}/proches")
    public ResponseEntity<List<ProcheResponse>> getProches(
            @PathVariable String userId) {
        return ResponseEntity.ok(patientProfileUseCase.getProches(userId));
    }

    @GetMapping("/{userId}/proches/{procheId}/profil")
    public ResponseEntity<PatientProfileResponse> getProcheProfile(
            @PathVariable String userId,
            @PathVariable String procheId) {
        return ResponseEntity.ok(patientProfileUseCase.getProcheProfile(userId, procheId));
    }

    @DeleteMapping("/{userId}/proches/{procheId}")
    public ResponseEntity<Void> deleteProche(
            @PathVariable String userId,
            @PathVariable String procheId) {
        patientProfileUseCase.deleteProche(userId, procheId);
        return ResponseEntity.noContent().build();
    }

    private Integer calculateAge(LocalDate dateNaissance) {
        if (dateNaissance == null) {
            return null;
        }
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    private RecommendationPatientContextResponse buildRecommendationContext(String userId) {
        PatientProfileResponse profile = patientProfileUseCase.getProfile(userId);
        List<String> conditions = profile.getMaladiesChroniques() != null
                ? profile.getMaladiesChroniques()
                : List.of();

        return RecommendationPatientContextResponse.builder()
                .userId(profile.getUserId())
                .age(calculateAge(profile.getDateNaissance()))
                .pregnant(Boolean.TRUE.equals(profile.getEnceinte()))
                .allergies(profile.getAllergies() != null ? profile.getAllergies() : List.of())
                .conditions(conditions)
                .maladiesChroniques(conditions)
                .groupeSanguin(profile.getGroupeSanguin())
                .build();
    }
}
