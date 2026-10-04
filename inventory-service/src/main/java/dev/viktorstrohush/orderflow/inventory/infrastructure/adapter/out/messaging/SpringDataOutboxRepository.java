package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, Long> {

    /**
     * Siguiente lote pendiente, en orden de inserción. SKIP LOCKED hace que otra instancia del
     * relay salte las filas que esta ya tiene bloqueadas en lugar de esperar o enviarlas dos veces.
     */
    @Query(value = "SELECT * FROM outbox WHERE published_at IS NULL ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxJpaEntity> lockPending(int limit);
}
