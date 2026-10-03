package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataReservationRepository extends JpaRepository<ReservationJpaEntity, UUID> {
}
