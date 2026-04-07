
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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MedicationClientDev
 *
 * Version de développement de MedicationClientPort.
 * Active avec le profil "dev".
 * Retourne des données fictives sans appeler medication-service.
 */
@Component
@Profile("dev")
@Primary  // Prioritaire sur MedicationRestClient en mode dev
public class MedicationRestClient implements MedicationClientPort {

    private static final Logger log = LoggerFactory.getLogger(MedicationRestClient.class);

    @Override
    public List<String> getContreIndications(String medicamentId) {
        log.info("🧪 [DEV] Simulation contre-indications pour medicamentId: {}", medicamentId);

        // Simuler différentes réponses selon l'ID du médicament
        switch (medicamentId) {
            case "med001":
            case "PARACETAMOL":
                return Arrays.asList("Grossesse", "Insuffisance hépatique");

            case "med002":
            case "IBUPROFENE":
                return Arrays.asList("Grossesse", "Ulcère gastrique", "Asthme");

            case "med003":
            case "AMOXICILLINE":
                return Arrays.asList("Allergie aux pénicillines");

            default:
                return Collections.emptyList();
        }
    }
}