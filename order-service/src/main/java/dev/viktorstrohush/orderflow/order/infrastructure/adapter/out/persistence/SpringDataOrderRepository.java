package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
