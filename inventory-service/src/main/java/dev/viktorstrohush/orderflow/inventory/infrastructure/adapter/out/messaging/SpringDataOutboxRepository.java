package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface SpringDataOutboxRepository extends JpaRepository<OutboxJpaEntity, Long> {

    /**
     * Next pending batch, in insertion order. SKIP LOCKED makes another instance of the relay
     * skip the rows this one has already locked instead of waiting or sending them twice.
     */
    @Query(value = "SELECT * FROM outbox WHERE published_at IS NULL ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxJpaEntity> lockPending(int limit);
}
