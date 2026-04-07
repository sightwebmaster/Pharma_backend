// ─────────────────────────────────────────────────────────────
// FICHIER 3 : FcmConfig.java — Config Firebase
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

/**
 * FcmConfig — initialise Firebase Admin SDK.
 * Activé uniquement sur le profil "prod".
 * En dev → MockFcmClientAdapter est utilisé à la place.
 */
@Slf4j
@Configuration
@Profile("prod")
public class FcmConfig {

    @Value("${pharmaApp.fcm.service-account-path}")
    private String serviceAccountPath;

    private final ResourceLoader resourceLoader;

    public FcmConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase déjà initialisé");
                return;
            }

            InputStream serviceAccount =
                    resourceLoader.getResource(serviceAccountPath).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialisé avec succès");

        } catch (Exception e) {
            log.error("Erreur initialisation Firebase : {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}