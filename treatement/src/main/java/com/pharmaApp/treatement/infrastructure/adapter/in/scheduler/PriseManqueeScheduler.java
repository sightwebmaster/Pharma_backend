package com.pharmaApp.treatement.infrastructure.adapter.in.scheduler;

import com.pharmaApp.treatement.application.service.TraitementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PriseManqueeScheduler
 *
 * Adaptateur primaire — déclenche la détection des prises manquées.
 *
 * Toutes les 5 minutes (configurable dans application.yml),
 * il appelle TraitementService.detecterEtMarquerPrisesManquees().
 *
 * Pourquoi c'est un adaptateur primaire ?
 * → Il est un "driver" du système : il initie une action (comme un Controller HTTP).
 *   La différence est que le déclencheur est le temps, pas une requête HTTP.
 *
 * Il ne contient aucune logique métier — il délègue tout au service.
 */
@Component
public class PriseManqueeScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PriseManqueeScheduler.class);

    private final TraitementService traitementService;

    @Value("${pharmaApp.scheduler.tolerance-minutes:30}")
    private int toleranceMinutes;

    public PriseManqueeScheduler(TraitementService traitementService) {
        this.traitementService = traitementService;
    }

    /**
     * Exécuté toutes les 5 minutes (fixedDelayString lit depuis application.yml).
     * fixedDelay (pas fixedRate) : attend la fin du précédent avant de relancer.
     * Évite les exécutions concurrentes si la détection prend plus de 5 min.
     */
    @Scheduled(fixedDelayString = "${pharmaApp.scheduler.detection-interval-ms:300000}")
    public void detecterPrisesManquees() {
        log.debug("Scheduler démarré — détection prises manquées (tolérance={}min)",
                toleranceMinutes);

        try {
            traitementService.detecterEtMarquerPrisesManquees(toleranceMinutes);
        } catch (Exception e) {
            // On logue mais on ne propage pas — le scheduler ne doit jamais crasher
            log.error("Erreur lors de la détection des prises manquées : {}",
                    e.getMessage(), e);
        }
    }
}