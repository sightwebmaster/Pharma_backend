
/*
package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.AdherenceClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AdherenceRestClient
 *
 * Adaptateur secondaire — implémente AdherenceClientPort.
 * Appelle adherence-service (:8086).
 *
 * Retourne le taux d'observance recalculé après chaque entrée.
 * Si indisponible → retourne 100 (optimiste) et logue ERROR.
 * Cela évite de bloquer l'app et de déclencher de fausses alertes.
 */

/*
@Component
public class AdherenceRestClient implements AdherenceClientPort {

    private static final Logger log =
            LoggerFactory.getLogger(AdherenceRestClient.class);

    private final RestTemplate restTemplate;

    @Value("${pharmaApp.services.adherence-service}")
    private String adherenceServiceUrl;

    public AdherenceRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public int enregistrerEntree(String patientUserId,
                                 String priseId,
                                 String medicamentNom,
                                 String statut) {
        String url = adherenceServiceUrl + "/api/v1/adherence/entries";
        try {
            AdherenceEntryResponse response = restTemplate.postForObject(
                    url,
                    Map.of(
                            "patientUserId", patientUserId,
                            "priseId",       priseId,
                            "medicamentNom", medicamentNom,
                            "statut",        statut
                    ),
                    AdherenceEntryResponse.class
            );

            int taux = (response != null) ? response.tauxObservance() : 100;
            log.debug("Adherence enregistrée — patient={} statut={} taux={}%",
                    patientUserId, statut, taux);
            return taux;

        } catch (Exception e) {
            log.error("Échec enregistrement adherence pour patient={} : {}",
                    patientUserId, e.getMessage());
            return 100; // valeur optimiste — pas d'alerte false-positive
        }
    }

    /** DTO interne — réponse d'adherence-service */
/*
    private record AdherenceEntryResponse(int tauxObservance) {}
}


 */

package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.AdherenceClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AdherenceClientDev
 *
 * Version de développement de AdherenceClientPort.
 * Active avec le profil "dev".
 * Simule le calcul du taux d'observance sans appel à adherence-service.
 */
@Component
@Profile("dev")
@Primary
public class AdherenceRestClient implements AdherenceClientPort {

    private static final Logger log = LoggerFactory.getLogger(AdherenceRestClient.class);

    // Simuler une base de données en mémoire pour suivre l'observance par patient
    private final Map<String, PatientAdherenceData> adherenceData = new HashMap<>();

    @Override
    public int enregistrerEntree(String patientUserId, String priseId,
                                 String medicamentNom, String statut) {
        log.info("📊 [DEV] Enregistrement observance - patient: {} prise: {} médicament: {} statut: {}",
                patientUserId, priseId, medicamentNom, statut);

        // Récupérer ou créer les données du patient
        PatientAdherenceData data = adherenceData.computeIfAbsent(
                patientUserId, k -> new PatientAdherenceData());

        // Enregistrer cette prise
        data.enregistrerPrise(statut);

        // Calculer le taux simulé
        int taux = data.calculerTauxObservance();

        log.info("📈 [DEV] Taux d'observance pour patient {}: {}% ({} confirmées / {} totales)",
                patientUserId, taux, data.prisesConfirmees, data.prisesTotales);

        return taux;
    }

    /**
     * Classe interne pour simuler les données d'observance d'un patient
     */
    private static class PatientAdherenceData {
        private int prisesTotales = 0;
        private int prisesConfirmees = 0;

        public void enregistrerPrise(String statut) {
            prisesTotales++;
            if ("CONFIRMEE".equals(statut)) {
                prisesConfirmees++;
            }
        }

        public int calculerTauxObservance() {
            if (prisesTotales == 0) {
                return 100;
            }
            return (prisesConfirmees * 100) / prisesTotales;
        }
    }
}