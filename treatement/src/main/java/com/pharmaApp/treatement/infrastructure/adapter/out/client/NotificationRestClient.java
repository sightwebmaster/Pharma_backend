/*

package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.NotificationClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * NotificationRestClient
 *
 * Adaptateur secondaire — implémente NotificationClientPort.
 * Appelle notification-service (:8087).
 *
 * En cas d'échec : log ERROR mais ne bloque pas le flux principal.
 * La notification est best-effort — une prise manquée n'est pas annulée
 * si la notification échoue.
 */

/*
@Component
public class NotificationRestClient implements NotificationClientPort {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationRestClient.class);

    private final RestTemplate restTemplate;

    @Value("${pharmaApp.services.notification-service}")
    private String notificationServiceUrl;

    public NotificationRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void planifierRappels(String traitementId,
                                 String patientUserId,
                                 int nombrePrises) {
        String url = notificationServiceUrl + "/api/v1/notifications/rappels";
        try {
            restTemplate.postForObject(url, Map.of(
                    "traitementId",  traitementId,
                    "patientUserId", patientUserId,
                    "nombrePrises",  nombrePrises
            ), Void.class);
            log.info("Rappels planifiés — traitement={} prises={}", traitementId, nombrePrises);
        } catch (Exception e) {
            log.error("Échec planification rappels pour traitement={} : {}",
                    traitementId, e.getMessage());
        }
    }

    @Override
    public void envoyerAlerteProcheManquee(String patientUserId, String medicamentNom) {
        String url = notificationServiceUrl + "/api/v1/notifications/alerte-proche";
        try {
            restTemplate.postForObject(url, Map.of(
                    "patientUserId", patientUserId,
                    "medicamentNom", medicamentNom
            ), Void.class);
            log.info("Alerte proche envoyée — patient={} méd={}", patientUserId, medicamentNom);
        } catch (Exception e) {
            log.error("Échec envoi alerte proche pour patient={} : {}",
                    patientUserId, e.getMessage());
        }
    }

    @Override
    public void mettreAJourRappels(String traitementId, String nouveauStatut) {
        String url = notificationServiceUrl
                + "/api/v1/notifications/rappels/" + traitementId;
        try {
            restTemplate.put(url, Map.of("statut", nouveauStatut));
            log.info("Rappels mis à jour — traitement={} statut={}", traitementId, nouveauStatut);
        } catch (Exception e) {
            log.error("Échec mise à jour rappels pour traitement={} : {}",
                    traitementId, e.getMessage());
        }
    }
}


 */



package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.NotificationClientPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * NotificationClientDev
 *
 * Version de développement de NotificationClientPort.
 * Active avec le profil "dev".
 * Simule les appels à notification-service sans véritable appel HTTP.
 */
@Component
@Profile("dev")
@Primary
public class NotificationRestClient implements NotificationClientPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationRestClient.class);

    @Override
    public void planifierRappels(String traitementId, String patientUserId, int nombrePrises) {
        log.info("📱 [DEV] Planification de {} rappels pour traitement: {} - patient: {}",
                nombrePrises, traitementId, patientUserId);

        // Simuler un délai de traitement
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("✅ [DEV] Rappels planifiés avec succès");
    }

    @Override
    public void envoyerAlerteProcheManquee(String patientUserId, String medicamentNom) {
        log.info("⚠️ [DEV] Alerte envoyée au proche - patient: {} médicament: {}",
                patientUserId, medicamentNom);

        // Simuler une notification
        log.info("📨 [DEV] SMS envoyé au proche: 'Prise manquée pour {}'", medicamentNom);
    }

    @Override
    public void mettreAJourRappels(String traitementId, String nouveauStatut) {
        log.info("🔄 [DEV] Mise à jour rappels traitement: {} - nouveau statut: {}",
                traitementId, nouveauStatut);

        // Simuler différents comportements selon le statut
        switch (nouveauStatut) {
            case "TERMINE":
                log.info("✅ [DEV] Tous les rappels ont été annulés");
                break;
            case "INTERROMPU":
                log.info("⏸️ [DEV] Rappels suspendus");
                break;
            default:
                log.info("📅 [DEV] Planning des rappels mis à jour");
                break;
        }
    }
}