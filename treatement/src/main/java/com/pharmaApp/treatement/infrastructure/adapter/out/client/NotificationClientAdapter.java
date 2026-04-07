package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.NotificationClientPort;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NotificationClientAdapter — Kafka Outbox
 *
 * Publie les events de notification dans la table outbox.
 * notification-service (futur) consommera ces topics Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClientAdapter implements NotificationClientPort {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void planifierRappels(String traitementId, String patientUserId, int nombrePrises) {
        saveToOutbox(traitementId, "TraitementCree", Map.of(
                "traitementId",  traitementId,
                "patientUserId", patientUserId,
                "nombrePrises",  nombrePrises
        ));
        log.info("Outbox ← TraitementCree traitementId={} patient={}", traitementId, patientUserId);
    }

    @Override
    public void envoyerAlerteProcheManquee(String patientUserId, String medicamentNom) {
        saveToOutbox(patientUserId, "AlerteProche", Map.of(
                "patientUserId", patientUserId,
                "medicamentNom", medicamentNom
        ));
        log.warn("Outbox ← AlerteProche patient={} medicament={}", patientUserId, medicamentNom);
    }

    @Override
    public void mettreAJourRappels(String traitementId, String nouveauStatut) {
        saveToOutbox(traitementId, "TraitementModifie", Map.of(
                "traitementId",  traitementId,
                "nouveauStatut", nouveauStatut
        ));
        log.info("Outbox ← TraitementModifie traitementId={} statut={}", traitementId, nouveauStatut);
    }

    private void saveToOutbox(String aggregateId, String eventType, Map<String, Object> data) {
        try {
            String payload = objectMapper.writeValueAsString(data);
            outboxRepository.save(OutboxEventEntity.of(
                    aggregateId,
                    "Traitement",
                    eventType,
                    payload
            ));
        } catch (Exception e) {
            log.error("Erreur écriture outbox eventType={} : {}", eventType, e.getMessage());
        }
    }
}