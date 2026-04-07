package com.pharmaApp.adherence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BC5 — Adherence Tracking Service
 * Port 8086
 *
 * Responsabilités :
 *  - Enregistrer chaque prise (confirmée ou manquée) reçue par événement Kafka
 *  - Calculer le taux d'observance sur 7j / 30j / 90j
 *  - Publier AlerteObservanceCritique si taux < 70%
 *  - Publier AlerteConsecutiveMissed si 3 prises manquées consécutives
 *  - Exposer l'historique au patient, pharmacien et proche
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class AdherenceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdherenceServiceApplication.class, args);
    }
}
