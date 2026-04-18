
// ─────────────────────────────────────────────────────────────
// FICHIER 3 : PriseManqueeConsumer.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.input.kafka;

import com.pharmaApp.Notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consomme prise.manquee → alerte le proche si présent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriseManqueeConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "prise.manquee",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriseManquee(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.warn("Kafka ← [{}] offset={}", topic, offset);

        try {
            String patientUserId = (String) payload.get("patientUserId");
            String medicamentNom = (String) payload.get("medicamentNom");
            String priseId       = (String) payload.get("priseId");
            LocalDateTime heurePrevue = parseDateTime(payload.get("heurePrevue"));

            notificationService.alerterProcheManquee(
                    patientUserId,
                    medicamentNom,
                    priseId,
                    heurePrevue);
        } catch (Exception e) {
            log.error("Erreur PriseManqueeConsumer : {}", e.getMessage(), e);
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        String str = value.toString();
        if (str.length() > 19) {
            str = str.substring(0, 19);
        }
        return LocalDateTime.parse(str);
    }
}
