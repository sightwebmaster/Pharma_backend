package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.AdherenceClientPort;
import com.pharmaApp.treatement.infrastructure.messaging.kafka.producer.TraitementKafkaProducer;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AdherenceClientAdapter — Kafka Outbox
 *
 * Au lieu d'appeler adherence-service directement (couplage fort),
 * on publie un event dans la table outbox.
 * Le scheduler TraitementKafkaProducer le transmet ensuite à Kafka.
 * adherence-service consomme le topic et recalcule le taux.
 *
 * Retourne 100 par défaut — le vrai taux sera calculé par adherence-service.
 * Si le taux est critique, adherence-service publie lui-même une alerte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdherenceClientAdapter implements AdherenceClientPort {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int enregistrerEntree(String patientUserId, String priseId,
                                 String medicamentNom, String statut) {
        try {
            // Construit le payload JSON
            String payload = objectMapper.writeValueAsString(Map.of(
                    "priseId",       priseId,
                    "patientUserId", patientUserId,
                    "medicamentNom", medicamentNom,
                    "statut",        statut  // "CONFIRMEE" | "MANQUEE"
            ));

            // Topic selon le statut
            String eventType = "CONFIRMEE".equals(statut)
                    ? "PriseConfirmee"
                    : "PriseManquee";

            // Écrit dans outbox — même transaction que la DB
            outboxRepository.save(OutboxEventEntity.of(
                    priseId,
                    "Traitement",
                    eventType,
                    payload
            ));

            log.info("Outbox ← {} priseId={} patient={}", eventType, priseId, patientUserId);

        } catch (Exception e) {
            // Fail-Open : on logue mais on ne bloque pas le flux métier
            log.error("Erreur écriture outbox adherence priseId={} : {}", priseId, e.getMessage());
        }

        // Le vrai taux sera calculé par adherence-service
        // On retourne 100 pour ne pas déclencher d'alerte côté treatment
        return 100;
    }
}