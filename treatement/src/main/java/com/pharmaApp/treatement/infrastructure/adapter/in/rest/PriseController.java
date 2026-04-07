package com.pharmaApp.treatement.infrastructure.adapter.in.rest;

import com.pharmaApp.treatement.application.dto.ConfirmerPriseCommand;
import com.pharmaApp.treatement.application.port.in.ConfirmerPriseUseCase;
import com.pharmaApp.treatement.domain.exception.AccesDeniedDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PriseController
 *
 * Adaptateur primaire — gère la confirmation des prises par le patient.
 * Séparé de TraitementController car les acteurs sont différents :
 *   - TraitementController : pharmacien
 *   - PriseController      : patient
 */
@RestController
@RequestMapping("/api/v1/treatments")
public class PriseController {

    private final ConfirmerPriseUseCase confirmerUseCase;

    public PriseController(ConfirmerPriseUseCase confirmerUseCase) {
        this.confirmerUseCase = confirmerUseCase;
    }

    // ================================================================
    // POST /api/v1/treatments/{id}/prises/{priseId}/confirm
    // Rôle requis : PATIENT
    // ================================================================

    @PostMapping("/{traitementId}/prises/{priseId}/confirm")
    public ResponseEntity<Void> confirmerPrise(
            @PathVariable String traitementId,
            @PathVariable String priseId,
            Authentication auth) {

        // L'ID du patient vient du JWT — pas du body (sécurité)
        String patientId = auth.getName();

        confirmerUseCase.confirmer(new ConfirmerPriseCommand(
                traitementId,
                priseId,
                patientId
        ));

        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(AccesDeniedDomainException.class)
    public ResponseEntity<String> handleAccesDenied(AccesDeniedDomainException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}