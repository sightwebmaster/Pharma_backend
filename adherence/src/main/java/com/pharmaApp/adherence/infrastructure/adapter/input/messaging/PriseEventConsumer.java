package com.pharmaApp.adherence.infrastructure.adapter.input.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;

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
    private final ObjectMapper objectMapper;  // injecté par Spring Boot

    @KafkaListener(topics = "prise.confirmee", groupId = "adherence-service-group")
    public void onPriseConfirmee(
            @Payload String rawPayload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) throws Exception {

        // ✅ Désérialisation manuelle — gère le double échappement
        String json = rawPayload;
        // Si double-échappé (commence par guillemet), on unescape
        if (json.startsWith("\"") && json.endsWith("\"")) {
            json = objectMapper.readValue(json, String.class);
        }
        PriseStatusEvent event = objectMapper.readValue(json, PriseStatusEvent.class);

        log.info("Kafka ← [{}] offset={} priseId={} patient={}",
                topic, offset, event.priseId(), event.patientUserId());

        try {
            adherenceUseCase.enregistrerPrise(toRequest(event, "CONFIRMEE"));
        } catch (Exception e) {
            log.error("Erreur PriseConfirmee priseId={} : {}", event.priseId(), e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "prise.manquee", groupId = "adherence-service-group")
    public void onPriseManquee(
            @Payload String rawPayload,
            @Header(KafkaHeaders.OFFSET) long offset) throws Exception {

        String json = rawPayload;
        if (json.startsWith("\"") && json.endsWith("\"")) {
            json = objectMapper.readValue(json, String.class);
        }
        PriseStatusEvent event = objectMapper.readValue(json, PriseStatusEvent.class);

        log.warn("Kafka ← [prise.manquee] offset={} priseId={}", offset, event.priseId());

        try {
            adherenceUseCase.enregistrerPrise(toRequest(event, "MANQUEE"));
        } catch (Exception e) {
            log.error("Erreur PriseManquee priseId={} : {}", event.priseId(), e.getMessage(), e);
        }
    }

    private EnregistrerPriseRequest toRequest(PriseStatusEvent event, String statut) {
        return EnregistrerPriseRequest.builder()
                .priseMedicamentId(event.priseId())
                .traitementId(event.traitementId())
                .patientUserId(event.patientUserId())
                .pharmacienUserId(event.pharmacienUserId())
                .medicamentNom(event.medicamentNom())
                .dosage(event.dosage())
                .heurePrise(event.heurePrevue() != null ? event.heurePrevue().toLocalTime() : null)
                .heureConfirmation(event.heureReelle() != null ? event.heureReelle().toLocalTime() : null)
                .delaiMinutes(calculerDelai(event.heurePrevue(), event.heureReelle()))
                .datePrise(event.heurePrevue() != null ? event.heurePrevue().toLocalDate() : LocalDate.now())
                .statut(statut)
                .build();
    }

    private Integer calculerDelai(LocalDateTime prevue, LocalDateTime reelle) {
        if (prevue == null || reelle == null) return null;
        return (int) java.time.Duration.between(prevue, reelle).toMinutes();
    }
}