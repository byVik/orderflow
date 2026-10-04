package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationRepository;
import dev.viktorstrohush.orderflow.inventory.domain.model.Reservation;
import dev.viktorstrohush.orderflow.inventory.domain.model.ReservationRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ReservationRepositoryAdapter implements ReservationRepository {

    private final SpringDataReservationRepository jpa;

    ReservationRepositoryAdapter(SpringDataReservationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Reservation> find(UUID orderId) {
        return jpa.findById(orderId).map(ReservationRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Reservation reservation) {
        ReservationJpaEntity entity = jpa.findById(reservation.orderId())
                .map(existing -> {
                    existing.updateState(reservation.status(), reservation.reason());
                    return existing;
                })
                .orElseGet(() -> toNewEntity(reservation));
        jpa.save(entity);
    }

    private static ReservationJpaEntity toNewEntity(Reservation reservation) {
        List<ReservationItemEmbeddable> items = reservation.items().stream()
                .map(i -> new ReservationItemEmbeddable(i.sku(), i.quantity()))
                .toList();
        return new ReservationJpaEntity(reservation.orderId(), reservation.status(), reservation.reason(),
                Instant.now(), items);
    }

    private static Reservation toDomain(ReservationJpaEntity e) {
        List<ReservationRequest.Item> items = e.getItems().stream()
                .map(i -> new ReservationRequest.Item(i.sku(), i.quantity()))
                .toList();
        return Reservation.restore(e.getOrderId(), e.getStatus(), e.getReason(), items);
    }
}
