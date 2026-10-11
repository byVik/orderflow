package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes the outbox's pending events to Kafka. It waits for the broker's acknowledgement before
 * marking each row; if a send fails, the transaction rolls back and the batch is retried on the
 * next pass. The guarantee is at-least-once: consumers must be idempotent.
 */
@Component
class OutboxRelay {

    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final SpringDataOutboxRepository outbox;
    private final KafkaTemplate<String, Object> kafka;

    OutboxRelay(SpringDataOutboxRepository outbox, KafkaTemplate<String, Object> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${orderflow.outbox.poll-interval-ms}")
    @Transactional
    public void publishPending() {
        for (OutboxJpaEntity message : outbox.lockPending(BATCH_SIZE)) {
            send(message);
            message.markPublished(Instant.now());
        }
    }

    private void send(OutboxJpaEntity message) {
        try {
            // The payload is already JSON: it is sent as text, without serializing it again (see KafkaConfig).
            kafka.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox send interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish event " + message.getId()
                    + " to " + message.getTopic() + "; it will be retried", e);
        }
    }
}
