package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adaptador de salida: traduce entre el agregado de dominio y las entidades JPA. */
@Component
class OrderPersistenceAdapter implements OrderRepository {

    private final SpringDataOrderRepository jpa;

    OrderPersistenceAdapter(SpringDataOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = jpa.findById(order.id().value())
                .map(existing -> {
                    existing.updateState(order.status(), order.rejectionReason());
                    return existing;
                })
                .orElseGet(() -> toNewEntity(order));
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(OrderPersistenceAdapter::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderPersistenceAdapter::toDomain)
                .toList();
    }

    private static OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(order.id().value(), order.customerId(), order.status(),
                order.rejectionReason(), order.createdAt());
        order.lines().forEach(l -> entity.addLine(new OrderLineJpaEntity(l.sku(), l.quantity(), l.unitPrice())));
        return entity;
    }

    private static Order toDomain(OrderJpaEntity e) {
        List<OrderLine> lines = e.getLines().stream()
                .map(l -> new OrderLine(l.getSku(), l.getQuantity(), l.getUnitPrice()))
                .toList();
        return Order.restore(new OrderId(e.getId()), e.getCustomerId(), lines, e.getStatus(),
                e.getRejectionReason(), e.getCreatedAt());
    }
}
