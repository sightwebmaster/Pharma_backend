package com.pharmaApp.medication.application.service;

import com.pharmaApp.medication.application.dto.request.*;
import com.pharmaApp.medication.application.dto.response.*;
import com.pharmaApp.medication.infrastructure.adapter.output.openfda.OpenFdaClient;
import com.pharmaApp.medication.infrastructure.adapter.output.persistence.entity.MedicamentEntity;
import com.pharmaApp.medication.infrastructure.adapter.output.persistence.repository.MedicamentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MedicamentService {

    private final MedicamentJpaRepository medicamentRepository;
    private final OpenFdaClient           openFdaClient;

    // =================================================================
    // UC1 — Recherche par nom ou principe actif
    // =================================================================

    @Cacheable(value = "medicaments", key = "#query.toLowerCase()")
    @Transactional(readOnly = true)
    public List<MedicamentResponse> rechercher(String query) {
        log.debug("Recherche médicament: '{}'", query);

        // 1. Cherche en DB interne
        List<MedicamentEntity> resultats =
                medicamentRepository.searchByNomOrPrincipeActif(query);

        if (!resultats.isEmpty()) {
            log.debug("Trouvé en DB interne: {} résultat(s)", resultats.size());
            return resultats.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // 2. Fallback OpenFDA
        log.debug("Pas trouvé en DB — appel OpenFDA pour: '{}'", query);
        List<MedicamentEntity> fromFda = openFdaClient.rechercher(query);

        if (!fromFda.isEmpty()) {
            // Sauvegarde en DB pour les prochaines recherches
            List<MedicamentEntity> saved = medicamentRepository.saveAll(fromFda);
            log.info("Sauvegardé {} médicament(s) depuis OpenFDA", saved.size());
            return saved.stream().map(this::toResponse).collect(Collectors.toList());
        }

        return List.of();
    }

    // =================================================================
    // UC2 — Recherche par symptômes
    // =================================================================

    @Transactional(readOnly = true)
    public List<MedicamentResponse> rechercherParSymptomes(List<String> symptomes) {
        log.debug("Recherche par symptômes: {}", symptomes);

        // Cherche dans DB interne tous les médicaments
        // dont au moins un symptôme correspond
        List<MedicamentEntity> tous = medicamentRepository.findAll();

        return tous.stream()
                .filter(med -> med.getSymptomes() != null &&
                        symptomes.stream().anyMatch(s ->
                                med.getSymptomes().stream()
                                        .anyMatch(ms -> ms.toLowerCase()
                                                .contains(s.toLowerCase()))))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =================================================================
    // UC3 — Vérification contre-indications
    // =================================================================

    @Transactional(readOnly = true)
    public ContreIndicationResponse verifierContreIndications(
            ContreIndicationRequest request) {

        MedicamentEntity med = medicamentRepository
                .findById(request.getMedicamentId())
                .orElseThrow(() -> new RuntimeException(
                        "Médicament introuvable : " + request.getMedicamentId()));

        List<String> alertes       = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();

        // ── 1. Vérification allergies ────────────────────────────
        if (request.getAllergies() != null && med.getAllergenes() != null) {
            for (String allergie : request.getAllergies()) {
                boolean match = med.getAllergenes().stream()
                        .anyMatch(a -> a.toLowerCase()
                                .contains(allergie.toLowerCase()));
                if (match) {
                    alertes.add("⚠️ ALLERGIE DÉTECTÉE : " +
                            "Le patient est allergique à " + allergie +
                            " contenu dans " + med.getNom());
                }
            }
        }

        // ── 2. Vérification maladies chroniques ──────────────────
        if (request.getMaladiesChroniques() != null &&
                med.getContreIndications() != null) {
            for (String maladie : request.getMaladiesChroniques()) {
                boolean match = med.getContreIndications().stream()
                        .anyMatch(ci -> ci.toLowerCase()
                                .contains(maladie.toLowerCase()));
                if (match) {
                    alertes.add("🚫 CONTRE-INDICATION : " +
                            med.getNom() + " est contre-indiqué en cas de " + maladie);
                }
            }
        }

        // ── 3. Vérification âge ──────────────────────────────────
        if (request.getAge() != null && med.getRestrictionsAge() != null) {
            if (request.getAge() < 12 &&
                    med.getRestrictionsAge().contains("enfant_moins_12")) {
                alertes.add("🚫 CONTRE-INDICATION : " +
                        med.getNom() + " est contre-indiqué pour les enfants de moins de 12 ans");
            }
            if (request.getAge() < 18 &&
                    med.getRestrictionsAge().contains("enfant_moins_18")) {
                avertissements.add("⚠️ Utilisation déconseillée avant 18 ans — consulter un médecin");
            }
        }

        // ── 4. Vérification grossesse ────────────────────────────
        if (request.isEnceinte() && med.getRestrictionsAge() != null &&
                med.getRestrictionsAge().contains("grossesse")) {
            alertes.add("🚫 CONTRE-INDICATION : " +
                    med.getNom() + " est contre-indiqué pendant la grossesse");
        }

        // ── 5. Vérification allaitement ──────────────────────────
        if (request.isAllaitement() && med.getRestrictionsAge() != null &&
                med.getRestrictionsAge().contains("allaitement")) {
            avertissements.add("⚠️ Prudence : " +
                    med.getNom() + " pendant l'allaitement — consulter un médecin");
        }

        boolean safe = alertes.isEmpty();
        log.info("Vérification CI — médicament={} safe={} alertes={}",
                med.getNom(), safe, alertes.size());

        return ContreIndicationResponse.builder()
                .safe(safe)
                .medicamentNom(med.getNom())
                .alertes(alertes)
                .avertissements(avertissements)
                .build();
    }

    // =================================================================
    // UC4 — Détail d'un médicament par ID
    // =================================================================

    @Cacheable(value = "medicament", key = "#id")
    @Transactional(readOnly = true)
    public MedicamentResponse getById(String id) {
        return medicamentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Médicament introuvable : " + id));
    }

    // =================================================================
    // UTILITAIRE
    // =================================================================

    private MedicamentResponse toResponse(MedicamentEntity e) {
        return MedicamentResponse.builder()
                .id(e.getId())
                .nom(e.getNom())
                .principeActif(e.getPrincipeActif())
                .dosage(e.getDosage())
                .forme(e.getForme())
                .description(e.getDescription())
                .symptomes(e.getSymptomes())
                .contreIndications(e.getContreIndications())
                .effetsSecondaires(e.getEffetsSecondaires())
                .allergenes(e.getAllergenes())
                .restrictionsAge(e.getRestrictionsAge())
                .prix(e.getPrix())
                .source(e.getSource())
                .build();
    }
}