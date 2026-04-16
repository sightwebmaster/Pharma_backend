package com.pharmaApp.treatement.infrastructure.adapter.in.rest;

import com.pharmaApp.treatement.application.dto.*;
import com.pharmaApp.treatement.application.port.in.*;
import com.pharmaApp.treatement.domain.exception.AccesDeniedDomainException;
import com.pharmaApp.treatement.domain.exception.ConflitTraitementException;
import com.pharmaApp.treatement.domain.exception.TransitionStatutInvalideException;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * TraitementController
 *
 * Adaptateur primaire — reçoit les requêtes HTTP REST.
 *
 * Responsabilités :
 *   1. Extraire le rôle et l'ID de l'acteur depuis le JWT (Authentication)
 *   2. Construire la Command avec ces informations
 *   3. Appeler le Use Case correspondant (port entrant)
 *   4. Retourner la réponse HTTP appropriée
 *   5. Gérer les exceptions domaine → codes HTTP
 *
 * Ce controller ne contient AUCUNE logique métier.
 * Il ne connaît pas TraitementService — il parle aux interfaces UseCase.
 */
@RestController
@RequestMapping("/api/v1/treatments")
public class TraitementController {

    private final PlanifierTraitementUseCase  planifierUseCase;
    private final ModifierTraitementUseCase   modifierUseCase;
    private final GetTraitementActifUseCase   getActifUseCase;
    private final GetPrisesAujourdhuiUseCase getPrisesAujourdhuiUseCase;


    public TraitementController(
            PlanifierTraitementUseCase planifierUseCase,
            ModifierTraitementUseCase  modifierUseCase,
            GetTraitementActifUseCase  getActifUseCase,
            GetPrisesAujourdhuiUseCase    getPrisesAujourdhuiUseCase) {
        this.planifierUseCase = planifierUseCase;
        this.modifierUseCase  = modifierUseCase;
        this.getActifUseCase  = getActifUseCase;
        this.getPrisesAujourdhuiUseCase = getPrisesAujourdhuiUseCase;
    }

    // ================================================================
    // POST /api/v1/treatments — Planifier un traitement
    // Rôle requis : PHARMACIEN
    // ================================================================

    @PostMapping
    public ResponseEntity<TraitementResponse> planifier(
            @RequestBody @Valid PlanifierRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        // ✅ Priorité : header X-User-Id (Gateway) → sinon body acteurId
        String acteurId   = userId    != null ? userId    : request.acteurId();
        // ✅ Priorité : header X-User-Role (Gateway) → sinon body acteurRole
        String acteurRole = userRole  != null ? userRole  : request.acteurRole();

        PlanifierTraitementCommand command = new PlanifierTraitementCommand(
                acteurId,
                acteurRole,
                request.patientUserId(),
                request.dateDebut(),
                request.dateFin(),
                request.motif(),
                request.lignes()
        );

        TraitementResponse response = planifierUseCase.planifier(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================================================================
    // PUT /api/v1/treatments/{id}/statut — Modifier le statut
    // Rôle requis : PHARMACIEN
    // ================================================================

    @PutMapping("/{traitementId}/statut")
    public ResponseEntity<TraitementResponse> modifierStatut(
            @PathVariable String traitementId,
            @RequestBody ModifierStatutRequest request,
            Authentication auth) {

        ModifierTraitementCommand command = new ModifierTraitementCommand(
                traitementId,
                auth.getName(),
                extraireRole(auth),
                request.nouveauStatut(),
                request.motif()
        );

        return ResponseEntity.ok(modifierUseCase.modifier(command));
    }




    // ================================================================
    // GET /api/v1/treatments/patient/{patientId}/active
    // Appelé par recommendation-service pour enrichir contexte IA
    // ================================================================

    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<TraitementResponse> getTraitementActif(
            @PathVariable String patientId) {
        return ResponseEntity.ok(getActifUseCase.getTraitementActif(patientId));
    }

    @GetMapping("/patient/{patientId}/prises/today")
    public ResponseEntity<List<PriseResponse>> getPrisesAujourdhui(
            @PathVariable String patientId) {
        return ResponseEntity.ok(
                getPrisesAujourdhuiUseCase.getPrisesAujourdhui(patientId)
        );
    }



    // ================================================================
    // Gestion des exceptions domaine → HTTP
    // ================================================================

    @ExceptionHandler(AccesDeniedDomainException.class)
    public ResponseEntity<ErrorResponse> handleAccesDenied(AccesDeniedDomainException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCES_REFUSE", e.getMessage()));
    }

    @ExceptionHandler(ConflitTraitementException.class)
    public ResponseEntity<ErrorResponse> handleConflit(ConflitTraitementException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLIT_PRINCIPE_ACTIF", e.getMessage()));
    }

    @ExceptionHandler(TransitionStatutInvalideException.class)
    public ResponseEntity<ErrorResponse> handleTransition(TransitionStatutInvalideException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("TRANSITION_INVALIDE", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("INTROUVABLE", e.getMessage()));
    }

    // ================================================================
    // Utilitaire — extraction du rôle depuis Spring Security
    // ================================================================

    /**
     * Extrait le premier rôle de l'Authentication Spring Security.
     * Les rôles sont préfixés "ROLE_" par Spring — on enlève le préfixe.
     * Ex : "ROLE_PHARMACIEN" → "PHARMACIEN"
     */
    private String extraireRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .findFirst()
                .orElse("UNKNOWN");
    }

    // ================================================================
    // DTOs de requête internes au Controller
    // ================================================================

    record PlanifierRequest(
            String acteurId,       // ✅ matche "acteurId"
            String patientUserId,  // ✅ matche "patientUserId"
            String acteurRole,     // ✅ matche "acteurRole"
            java.time.LocalDate dateDebut,
            java.time.LocalDate dateFin,
            String motif,
            java.util.List<PlanifierTraitementCommand.LigneMedicament> lignes
    ) {}

    record ModifierStatutRequest(
            String nouveauStatut,
            String motif
    ) {}

    record ErrorResponse(String code, String message) {}
}