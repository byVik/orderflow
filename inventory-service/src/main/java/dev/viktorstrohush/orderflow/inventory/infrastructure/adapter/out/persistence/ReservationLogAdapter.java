package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationLog;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class ReservationLogAdapter implements ReservationLog {

    private final SpringDataReservationRepository jpa;

    ReservationLogAdapter(SpringDataReservationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ReservationResult> find(UUID orderId) {
        return jpa.findById(orderId).map(e -> new ReservationResult(e.getOrderId(), e.isReserved(), e.getReason()));
    }

    @Override
    public void record(ReservationResult result) {
        jpa.save(new ReservationJpaEntity(result.orderId(), result.reserved(), result.reason(), Instant.now()));
    }
}
