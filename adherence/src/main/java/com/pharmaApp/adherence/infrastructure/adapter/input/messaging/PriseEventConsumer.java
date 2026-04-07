package com.pharmaApp.adherence.infrastructure.adapter.input.messaging;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.domain.event.PriseStatusEvent;
import com.pharmaApp.adherence.domain.port.input.AdherenceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * PriseEventConsumer — Adaptateur d'entrée Kafka
 *
 * Consomme les events de treatment-service et les convertit
 * en EnregistrerPriseRequest pour le use case.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriseEventConsumer {

    private final AdherenceUseCase adherenceUseCase;

    @KafkaListener(
            topics = "prise.confirmee",
            groupId = "adherence-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriseConfirmee(
            @Payload PriseStatusEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Kafka ← [{}] offset={} priseId={} patient={}",
                topic, offset, event.priseId(), event.patientUserId());

        try {
            adherenceUseCase.enregistrerPrise(toRequest(event, "CONFIRMEE"));
        } catch (Exception e) {
            log.error("Erreur PriseConfirmee priseId={} : {}",
                    event.priseId(), e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "prise.manquee",
            groupId = "adherence-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriseManquee(
            @Payload PriseStatusEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.warn("Kafka ← [{}] offset={} priseId={} patient={}",
                topic, offset, event.priseId(), event.patientUserId());

        try {
            adherenceUseCase.enregistrerPrise(toRequest(event, "MANQUEE"));
        } catch (Exception e) {
            log.error("Erreur PriseManquee priseId={} : {}",
                    event.priseId(), e.getMessage(), e);
        }
    }

    private EnregistrerPriseRequest toRequest(PriseStatusEvent event, String statut) {
        return EnregistrerPriseRequest.builder()
                .priseMedicamentId(event.priseId())
                .traitementId(event.traitementId())
                .patientUserId(event.patientUserId())
                .medicamentNom(event.medicamentNom())
                .statut(statut)
                .datePrise(LocalDate.now())  // Déduit de now() — Kafka ne transmet pas la date
                .build();
    }
}