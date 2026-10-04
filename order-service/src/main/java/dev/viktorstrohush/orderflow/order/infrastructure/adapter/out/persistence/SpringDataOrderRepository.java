package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    /** El grafo trae las líneas en la misma consulta (join) y evita una consulta por pedido (N+1). */
    @EntityGraph(attributePaths = "lines")
    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
