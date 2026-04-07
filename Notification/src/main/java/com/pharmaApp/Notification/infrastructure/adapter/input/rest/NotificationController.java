// ─────────────────────────────────────────────────────────────
// FICHIER 1 : NotificationController.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.input.rest;

import com.pharmaApp.Notification.application.service.NotificationService;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.NotificationEnvoyeeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NotificationController — derrière le Gateway.
 * Lit X-User-Id depuis les headers (injecté par Gateway).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * POST /api/v1/notifications/device-token
     * Enregistre ou met à jour le token FCM d'un utilisateur.
     */
    @PostMapping("/device-token")
    public ResponseEntity<Void> enregistrerToken(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {

        String deviceToken = body.get("deviceToken");
        String plateforme  = body.getOrDefault("plateforme", "ANDROID");

        notificationService.enregistrerToken(userId, deviceToken, plateforme);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/notifications/historique/{patientId}
     * Retourne les 50 dernières notifications d'un patient.
     */
    @GetMapping("/historique/{patientId}")
    public ResponseEntity<List<NotificationEnvoyeeEntity>> getHistorique(
            @PathVariable String patientId,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        // Un patient ne peut voir que ses propres notifications
        if ("PATIENT".equalsIgnoreCase(userRole) && !userId.equals(patientId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(notificationService.getHistorique(patientId));
    }

    /**
     * POST /api/v1/notifications/test-fcm (DEV ONLY)
     * Teste l'envoi FCM directement avec un token mock.
     */
    @PostMapping("/test-fcm")
    public ResponseEntity<String> testFcm(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> body) {

        String deviceToken = body.getOrDefault("deviceToken", "mock-token-test");
        notificationService.enregistrerToken(userId, deviceToken, "ANDROID");

        return ResponseEntity.ok("Token enregistré pour userId=" + userId);
    }
}