// ─────────────────────────────────────────────────────────────
// FICHIER 2 : MedicamentController.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.medication.infrastructure.adapter.input.rest;



import com.pharmaApp.medication.application.dto.response.*;
import com.pharmaApp.medication.application.dto.request.*;
import com.pharmaApp.medication.application.service.MedicamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicamentController {

    private final MedicamentService medicamentService;

    /**
     * GET /api/v1/medications/search?q=doliprane
     * Recherche par nom ou principe actif.
     * Cascade : Redis → MySQL → OpenFDA
     */
    @GetMapping("/search")
    public ResponseEntity<List<MedicamentResponse>> search(
            @RequestParam String q) {
        log.info("Recherche médicament: '{}'", q);
        return ResponseEntity.ok(medicamentService.rechercher(q));
    }

    /**
     * POST /api/v1/medications/search/symptoms
     * Recherche par symptômes du patient.
     * Body: ["maux de tête", "fièvre"]
     */
    @PostMapping("/search/symptoms")
    public ResponseEntity<List<MedicamentResponse>> searchBySymptoms(
            @RequestBody List<String> symptomes) {
        log.info("Recherche par symptômes: {}", symptomes);
        return ResponseEntity.ok(
                medicamentService.rechercherParSymptomes(symptomes));
    }

    /**
     * GET /api/v1/medications/{id}
     * Détail d'un médicament par ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MedicamentResponse> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(medicamentService.getById(id));
    }

    // Dans MedicamentController.java — ajouter cette méthode
    /**
     * GET /api/v1/medications/{id}/contre-indications
     * Appelé par treatment-service (Feign) — retourne la liste des CI
     */
    @GetMapping("/{id}/contre-indications")
    public ResponseEntity<List<String>> getContreIndications(
            @PathVariable String id) {
        log.info("Contre-indications pour medicamentId={}", id);
        MedicamentResponse med = medicamentService.getById(id);
        return ResponseEntity.ok(
                med.getContreIndications() != null
                        ? med.getContreIndications()
                        : List.of()
        );
    }

    /**
     * POST /api/v1/medications/check-contre-indications
     * Vérifie les contre-indications pour un patient.
     * Body: { medicamentId, allergies[], maladiesChroniques[], age, enceinte }
     */
    @PostMapping("/check-contre-indications")
    public ResponseEntity<ContreIndicationResponse> checkContreIndications(
            @Valid @RequestBody ContreIndicationRequest request) {
        log.info("Vérification CI — médicament={}", request.getMedicamentId());
        return ResponseEntity.ok(
                medicamentService.verifierContreIndications(request));
    }
}