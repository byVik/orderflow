package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** An event waiting to be published. It is inserted in the same transaction as the business change. */
@Entity
@Table(name = "outbox")
class OutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxJpaEntity() {
    }

    OutboxJpaEntity(String topic, String messageKey, String payload, Instant createdAt) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    void markPublished(Instant when) {
        this.publishedAt = when;
    }

    Long getId() { return id; }
    String getTopic() { return topic; }
    String getMessageKey() { return messageKey; }
    String getPayload() { return payload; }
}
