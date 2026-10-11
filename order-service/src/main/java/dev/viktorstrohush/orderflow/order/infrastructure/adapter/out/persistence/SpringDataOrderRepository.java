package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    /** The graph fetches the lines in the same query (join) and avoids one query per order (N+1). */
    @EntityGraph(attributePaths = "lines")
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
