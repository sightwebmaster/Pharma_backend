
/*
package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.MedicationClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;

/**
 * MedicationRestClient
 *
 * Adaptateur secondaire — implémente MedicationClientPort.
 * Appelle medication-service (:8083) via RestTemplate.
 *
 * Comportement Fail-Open (choix métier délibéré) :
 * Si medication-service est indisponible → log WARNING + liste vide retournée.
 * Le traitement est créé sans vérification de contre-indications.
 */
/*
@Component
public class MedicationRestClient implements MedicationClientPort {

    private static final Logger log =
            LoggerFactory.getLogger(MedicationRestClient.class);

    private final RestTemplate restTemplate;

    @Value("${pharmaApp.services.medication-service}")
    private String medicationServiceUrl;

    public MedicationRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<String> getContreIndications(String medicamentId) {
        String url = medicationServiceUrl
                + "/api/v1/medications/" + medicamentId + "/contraindications";

        try {
            log.debug("Appel medication-service : GET {}", url);

            ContraIndicationsResponse response = restTemplate.getForObject(
                    url, ContraIndicationsResponse.class);

            if (response == null || response.contraindications() == null) {
                return Collections.emptyList();
            }

            return response.contraindications();

        } catch (ResourceAccessException e) {
            // Fail-Open : service indisponible — on continue sans vérification
            log.warn("medication-service indisponible pour medicamentId={} — " +
                    "traitement créé sans vérification contre-indications. " +
                    "Cause : {}", medicamentId, e.getMessage());
            return Collections.emptyList();

        } catch (Exception e) {
            // Toute autre erreur (404, parsing, timeout) — même comportement
            log.warn("Erreur lors de la récupération des contre-indications " +
                    "pour medicamentId={} : {}", medicamentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** DTO interne pour désérialiser la réponse JSON de medication-service */
/*
    private record ContraIndicationsResponse(List<String> contraindications) {}
}
*/


package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.MedicationClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * MedicationClientDev
 *
 * Version de développement de MedicationClientPort.
 * Active avec le profil "dev".
 * Retourne des données fictives sans appeler medication-service.
 */
@Component
@Primary
public class MedicationRestClient implements MedicationClientPort {

    private static final Logger log = LoggerFactory.getLogger(MedicationRestClient.class);
    private final RestTemplate restTemplate;

    @Value("${pharmaApp.services.medication-service:http://localhost:8088}")
    private String medicationServiceUrl;

    public MedicationRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<String> getContreIndications(String medicamentId) {
        // ✅ Fail-Open — si medication-service down → liste vide
        if (medicamentId == null || medicamentId.isBlank()) {
            return Collections.emptyList();
        }

        String url = medicationServiceUrl
                + "/api/v1/medications/" + medicamentId + "/contre-indications";
        try {
            ContreIndicationsResponse response = restTemplate.getForObject(
                    url, ContreIndicationsResponse.class);
            return (response != null && response.contreIndications() != null)
                    ? response.contreIndications()
                    : Collections.emptyList();
        } catch (Exception e) {
            log.warn("medication-service indisponible pour medicamentId={} — Fail-Open. Cause: {}",
                    medicamentId, e.getMessage());
            return Collections.emptyList();
        }
    }
    @Override
    public Optional<String> getMedicamentIdByNom(String nom) {
        if (nom == null || nom.isBlank()) return Optional.empty();

        String url = medicationServiceUrl
                + "/api/v1/medications/search?q="
                + nom.replace(" ", "%20");
        try {
            MedicamentDTO[] results = restTemplate.getForObject(url, MedicamentDTO[].class);
            if (results != null && results.length > 0) {
                return Optional.ofNullable(results[0].id());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Impossible de résoudre medicamentId pour '{}' — Fail-Open. Cause: {}",
                    nom, e.getMessage());
            return Optional.empty();
        }
    }

    private record MedicamentDTO(String id, String nom, String principeActif) {}

    private record ContreIndicationsResponse(List<String> contreIndications) {}
}