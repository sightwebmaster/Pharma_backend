// ─────────────────────────────────────────────────────────────
// FICHIER 1 : TraitementCreeConsumer.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.input.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaApp.Notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Consomme traitement.cree → planifie les rappels FCM en base.
 * Idempotent : si rappels existent déjà pour ce traitementId → ignore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraitementCreeConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "traitement.cree",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTraitementCree(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Kafka ← [{}] offset={} payload={}", topic, offset, payload);

        try {
            String traitementId  = (String) payload.get("traitementId");
            String patientUserId = (String) payload.get("patientUserId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> prises =
                    (List<Map<String, Object>>) payload.get("prises");

            notificationService.planifierRappels(traitementId, patientUserId, prises);

        } catch (Exception e) {
            log.error("Erreur TraitementCreeConsumer : {}", e.getMessage(), e);
        }
    }
}