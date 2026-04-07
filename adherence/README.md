# 📊 adherence-service — BC5 PharmaCare

**Port 8086 | Spring Boot 3.5 | Java 17 | MySQL | Kafka**

## Responsabilités

Ce service implémente le **Bounded Context 5 — Adherence Tracking** selon le DDD.

| Quoi | Pourquoi séparé du treatment-service |
|------|--------------------------------------|
| **Treatment** = ce qui *DOIT* être fait | Planification |
| **Adherence** = ce qui *A ÉTÉ* fait | Réalité + statistiques |

## Architecture Hexagonale

```
┌─────────────────────────────────────────────────────────────┐
│  ENTRÉES (Adapters Input)                                   │
│  ├── Kafka Consumer  ← treatment.prise-confirmee            │
│  │                   ← treatment.prise-manquee              │
│  └── REST Controller ← GET/POST /adherence/**               │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION (Use Cases)                                    │
│  └── AdherenceService implements AdherenceUseCase           │
├─────────────────────────────────────────────────────────────┤
│  DOMAINE (Logique Métier Pure)                              │
│  ├── AdherenceRecord (Aggregate Root)                       │
│  │   ├── recalculateTaux() → 7j / 30j / 90j               │
│  │   ├── isTauxCritique(seuil)                             │
│  │   └── compterConsecutifManques()                         │
│  └── HistoriqueEntry (Value Object)                         │
├─────────────────────────────────────────────────────────────┤
│  SORTIES (Adapters Output)                                  │
│  ├── MySQL JPA (AdherenceRepositoryAdapter)                 │
│  └── Kafka Producer → adherence.historique-enregistre       │
│                      → adherence.alerte-observance-critique  │
│                      → adherence.alerte-consecutive-missed   │
└─────────────────────────────────────────────────────────────┘
```

## Événements Kafka

### Consommés (depuis treatment-service)
| Topic | Payload |
|-------|---------|
| `treatment.prise-confirmee` | `PriseStatusEvent{statut="CONFIRME"}` |
| `treatment.prise-manquee`   | `PriseStatusEvent{statut="MANQUE"}`   |

### Publiés (vers notification-service)
| Topic | Déclencheur |
|-------|-------------|
| `adherence.historique-enregistre`      | Toujours, après chaque prise |
| `adherence.alerte-observance-critique` | Taux global < 70% |
| `adherence.alerte-consecutive-missed`  | 3 prises manquées consécutives |

## Endpoints REST

| Méthode | URL | Rôle |
|---------|-----|------|
| `GET`  | `/adherence/{patientId}/traitement/{tid}` | Résumé observance |
| `GET`  | `/adherence/{patientId}/traitement/{tid}/historique` | Historique prises |
| `GET`  | `/adherence/pharmacien/summaries` | Tous les patients (pharmacien) |
| `GET`  | `/adherence/patient/summaries` | Tous mes traitements (patient) |
| `POST` | `/adherence/{patientId}/traitement/{tid}/recalculate` | Forcer recalcul |
| `POST` | `/adherence/prises` | Test direct (PHARMACIEN only) |

## Règles Métier

| Règle | Seuil | Événement déclenché |
|-------|-------|---------------------|
| Taux critique | < 70% | `AlerteObservanceCritique` → pharmacien + proche |
| Prises consécutives | >= 3 MANQUE | `AlerteConsecutiveMissed` → proche |
| Fenêtres calcul | 7j / 30j / 90j | Automatique après chaque prise |

## Lancer le service

```bash
# 1. Démarrer MySQL + Kafka
docker-compose up -d adherence-mysql kafka zookeeper

# 2. Lancer le service
./mvnw spring-boot:run

# 3. Tester
curl http://localhost:8086/actuator/health
```

## Variables d'environnement importantes

| Variable | Défaut | Description |
|----------|--------|-------------|
| `SPRING_DATASOURCE_URL` | jdbc:mysql://localhost:3306/adherence_db | MySQL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | http://localhost:8090/realms/pharma-realm | Keycloak |
| `ADHERENCE_RULES_CRITICAL_THRESHOLD` | 70 | Seuil critique (%) |
| `ADHERENCE_RULES_CONSECUTIVE_MISSED_LIMIT` | 3 | Prises manquées consécutives |
