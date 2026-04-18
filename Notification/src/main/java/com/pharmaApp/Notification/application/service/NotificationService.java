package com.pharmaApp.Notification.application.service;

import com.pharmaApp.Notification.application.dto.FcmResult;
import com.pharmaApp.Notification.application.port.out.FcmClientPort;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.*;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationService — Phase 2
 * - Planification rappels (Kafka → TraitementCreeConsumer)
 * - Scheduler envoi FCM (toutes les minutes)
 * - Alerte proche (Kafka → PriseManqueeConsumer)
 * - Enregistrement token FCM (REST)
 * - Historique notifications (REST)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final RappelJpaRepository             rappelRepository;
    private final TokenFcmJpaRepository           tokenFcmRepository;
    private final NotificationEnvoyeeJpaRepository historiqueRepository;
    private final FcmClientPort                   fcmClient;

    // =================================================================
    // UC1 — Planifier rappels (depuis TraitementCreeConsumer)
    // =================================================================

    public void planifierRappels(
            String traitementId,
            String patientUserId,
            List<Map<String, Object>> prises) {

        if (prises == null || prises.isEmpty()) {
            log.warn("planifierRappels — aucune prise pour traitementId={}", traitementId);
            return;
        }

        // Idempotence
        long existants = rappelRepository.countByTraitementId(traitementId);
        if (existants > 0) {
            log.info("Rappels déjà planifiés pour traitementId={} ({}) — ignoré",
                    traitementId, existants);
            return;
        }

        int count = 0;
        for (Map<String, Object> prise : prises) {
            try {
                String medicamentNom = (String) prise.get("medicamentNom");
                String heureEnvoiStr = (String) prise.get("heureEnvoi");

                LocalDateTime heureEnvoi = parseDateTime(heureEnvoiStr);

                RappelEntity rappel = new RappelEntity();
                rappel.setId(UUID.randomUUID().toString());
                rappel.setTraitementId(traitementId);
                rappel.setPatientUserId(patientUserId);
                rappel.setMedicamentNom(medicamentNom != null ? medicamentNom : "Médicament");
                rappel.setHeureEnvoi(heureEnvoi);
                rappel.setStatut("PLANIFIE");
                rappel.setNbTentatives(0);

                rappelRepository.save(rappel);
                count++;

            } catch (Exception e) {
                log.error("Erreur création rappel : {}", e.getMessage());
            }
        }

        log.info("planifierRappels — {} rappels créés pour traitementId={}", count, traitementId);
    }

    // =================================================================
    // UC2 — Scheduler envoi FCM (toutes les 60 secondes)
    // =================================================================

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void envoyerRappelsDus() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<RappelEntity> dus = rappelRepository
                .findByStatutAndHeureEnvoiLessThanEqual("PLANIFIE", maintenant);

        if (dus.isEmpty()) return;

        log.info("Scheduler — {} rappel(s) dû(s) à envoyer", dus.size());

        for (RappelEntity rappel : dus) {
            try {
                envoyerRappelFcm(rappel);
            } catch (Exception e) {
                log.error("Erreur envoi rappel id={} : {}", rappel.getId(), e.getMessage());
            }
        }
    }

    private void envoyerRappelFcm(RappelEntity rappel) {
        Optional<TokenFcmEntity> tokenOpt =
                tokenFcmRepository.findByUserIdAndActifTrue(rappel.getPatientUserId());

        if (tokenOpt.isEmpty()) {
            log.warn("Token FCM introuvable pour patient={} — rappel id={} marqué ECHEC",
                    rappel.getPatientUserId(), rappel.getId());
            rappel.setStatut("ECHEC");
            rappelRepository.save(rappel);
            return;
        }

        String token  = tokenOpt.get().getDeviceToken();
        String titre  = "Rappel médicament";
        String corps  = "Il est l'heure de prendre votre " + rappel.getMedicamentNom();

        FcmResult result = fcmClient.envoyer(token, titre, corps);

        if (result.success()) {
            rappel.setStatut("ENVOYE");
            persisterHistorique(rappel.getPatientUserId(), "RAPPEL_PRISE",
                    titre, corps, "ENVOYE", result.messageId());
        } else {
            rappel.setNbTentatives(rappel.getNbTentatives() + 1);
            if (rappel.getNbTentatives() >= 3) {
                rappel.setStatut("ECHEC");
                log.error("Rappel id={} — 3 tentatives échouées", rappel.getId());
            }
            // Sinon reste PLANIFIE → retry au prochain cycle
        }

        rappelRepository.save(rappel);
    }

    // =================================================================
    // UC3 — Annuler rappel quand prise confirmée
    // =================================================================

    public void annulerRappelPrise(String priseId) {
        // La prise confirmée annule le rappel correspondant
        // On identifie par patientUserId + heure proche — simplification Phase 2
        log.info("annulerRappelPrise — priseId={}", priseId);
        // TODO : stocker priseId dans RappelEntity pour annulation précise
    }

    // =================================================================
    // UC4 — Alerte proche (depuis PriseManqueeConsumer)
    // =================================================================

    public void alerterProcheManquee(
            String patientUserId,
            String medicamentNom,
            String priseId) {

        log.warn("alerterProcheManquee — patient={} medicament={}", patientUserId, medicamentNom);

        // TODO : récupérer procheUserId depuis user-service via Feign (Phase 3)
        // Pour l'instant on logue uniquement
        log.info("Alerte proche — FCM non envoyé (procheUserId non disponible sans Feign)");
    }

    // =================================================================
    // UC5 — Enregistrer token FCM (depuis NotificationController)
    // =================================================================

    public void enregistrerToken(String userId, String deviceToken, String plateforme) {
        // Upsert : si token existe → update, sinon insert
        Optional<TokenFcmEntity> existing =
                tokenFcmRepository.findByUserIdAndDeviceToken(userId, deviceToken);

        TokenFcmEntity token;
        if (existing.isPresent()) {
            token = existing.get();
            token.setActif(true);
            token.setUpdatedAt(LocalDateTime.now());
        } else {
            // Désactive les anciens tokens de cet utilisateur
            tokenFcmRepository.findByUserIdAndActifTrue(userId)
                    .ifPresent(old -> {
                        old.setActif(false);
                        tokenFcmRepository.save(old);
                    });

            token = new TokenFcmEntity();
            token.setId(UUID.randomUUID().toString());
            token.setUserId(userId);
            token.setDeviceToken(deviceToken);
            token.setPlateforme(plateforme != null ? plateforme : "ANDROID");
            token.setActif(true);
        }

        tokenFcmRepository.save(token);
        log.info("Token FCM enregistré pour userId={}", userId);
    }

    // =================================================================
    // UC6 — Historique notifications (depuis NotificationController)
    // =================================================================

    @Transactional(readOnly = true)
    public List<NotificationEnvoyeeEntity> getHistorique(String userId) {
        return historiqueRepository.findByUserIdOrderByEnvoyeADesc(
                userId, PageRequest.of(0, 50));
    }

    // =================================================================
    // UTILITAIRES
    // =================================================================

    private void persisterHistorique(String userId, String type,
                                     String titre, String corps,
                                     String statut, String fcmMessageId) {
        NotificationEnvoyeeEntity notif = new NotificationEnvoyeeEntity();
        notif.setId(UUID.randomUUID().toString());
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTitre(titre);
        notif.setCorps(corps);
        notif.setStatut(statut);
        notif.setFcmMessageId(fcmMessageId);
        notif.setEnvoyeA(LocalDateTime.now());
        historiqueRepository.save(notif);
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null) return LocalDateTime.now().plusHours(1);
        try {
            return LocalDateTime.parse(str.length() > 19 ? str.substring(0, 19) : str);
        } catch (Exception e) {
            return LocalDateTime.now().plusHours(1);
        }
    }
}