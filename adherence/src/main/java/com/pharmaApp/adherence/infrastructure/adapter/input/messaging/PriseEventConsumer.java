package com.pharmaApp.adherence.infrastructure.adapter.input.messaging;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.domain.event.PriseStatusEvent;
import com.pharmaApp.adherence.domain.port.input.AdherenceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Adaptateur d'entrée — consomme les événements Kafka publiés par treatment-service.
 *
 * Topics :
 *  treatment.prise-confirmee  → PriseStatusEvent{statut="CONFIRME"}
 *  treatment.prise-manquee    → PriseStatusEvent{statut="MANQUE"}
 *
 * Ces deux topics ont la même structure, seul le statut diffère.
 * Le consumer gère les deux avec un @KafkaListener multi-topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PriseEventConsumer {

    private final AdherenceUseCase adherenceUseCase;

    @KafkaListener(
        topics = {"treatment.prise-confirmee", "treatment.prise-manquee"},
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriseEvent(
            @Payload PriseStatusEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ) {
        log.info("📥 Kafka ← {} : patient={} traitement={} méd={} statut={}",
                topic, event.getPatientUserId(), event.getTraitementId(),
                event.getMedicamentNom(), event.getStatut());

        try {
            EnregistrerPriseRequest request = EnregistrerPriseRequest.builder()
                    .priseMedicamentId(event.getPriseMedicamentId())
                    .traitementId(event.getTraitementId())
                    .patientUserId(event.getPatientUserId())
                    .pharmacienUserId(event.getPharmacienUserId())
                    .medicamentNom(event.getMedicamentNom())
                    .dosage(event.getDosage())
                    .datePrise(event.getDatePrise())
                    .heurePrise(event.getHeurePrise())
                    .heureConfirmation(event.getHeureConfirmation())
                    .statut(event.getStatut())
                    .notePatient(event.getNotePatient())
                    .build();

            adherenceUseCase.enregistrerPrise(request);

            log.info("✅ Prise enregistrée — patient={} statut={}",
                    event.getPatientUserId(), event.getStatut());

        } catch (Exception e) {
            // On log l'erreur mais on NE relance PAS l'exception
            // pour éviter une boucle de retry infinie sur cet événement.
            // Un Dead Letter Topic (DLT) peut être configuré si besoin.
            log.error("❌ Erreur traitement événement Kafka [{}] patient={} : {}",
                    topic, event.getPatientUserId(), e.getMessage(), e);
        }
    }
}
