// ─────────────────────────────────────────────────────────────
// FICHIER 2 : BeanConfig.java — Active le scheduler
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active le scheduler pour l'envoi FCM toutes les 60 secondes.
 */
@Configuration
@EnableScheduling
public class BeanConfig {
}
 