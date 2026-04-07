package com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox Pattern — table outbox_events
 *
 * Principe : au lieu de publier directement dans Kafka (risque de perte),
 * on écrit l'event dans cette table dans la MÊME transaction que la DB.
 * Un scheduler lit cette table et publie dans Kafka.
 * Garantit la cohérence : si la DB commit → l'event sera publié.
 */
@Getter
@Setter
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_status",  columnList = "status"),
                @Index(name = "idx_outbox_created", columnList = "created_at")
        }
)
public class OutboxEventEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "aggregate_id", length = 36, nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", length = 100, nullable = false)
    private String aggregateType;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public enum OutboxStatus { PENDING, PROCESSED, FAILED }

    // ── Factory methods ───────────────────────────────────────────
    public static OutboxEventEntity of(
            String aggregateId,
            String aggregateType,
            String eventType,
            String payload) {
        OutboxEventEntity e = new OutboxEventEntity();
        e.aggregateId   = aggregateId;
        e.aggregateType = aggregateType;
        e.eventType     = eventType;
        e.payload       = payload;
        e.status        = OutboxStatus.PENDING;
        return e;
    }
}