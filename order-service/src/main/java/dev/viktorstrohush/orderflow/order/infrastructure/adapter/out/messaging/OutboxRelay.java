package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica en Kafka los eventos pendientes del outbox. Espera la confirmación del broker antes de
 * marcar cada fila; si un envío falla, la transacción hace rollback y el lote se reintenta en la
 * siguiente pasada. La garantía es at-least-once: los consumidores deben ser idempotentes.
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
            // El payload ya es JSON: se envía como texto, sin volver a serializarlo (ver KafkaConfig).
            kafka.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío del outbox interrumpido", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("No se pudo publicar el evento " + message.getId()
                    + " en " + message.getTopic() + "; se reintentará", e);
        }
    }
}
