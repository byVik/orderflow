package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.persistence;

import dev.viktorstrohush.orderflow.order.application.port.PageQuery;
import dev.viktorstrohush.orderflow.order.application.port.PageResult;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: translates between the domain aggregate and the JPA entities. */
@Component
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderRepository {

    /** The id breaks the ties, so two orders created at the same instant keep their place between requests. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SpringDataOrderRepository jpa;

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

    /** Here the application's PageQuery becomes Spring Data's PageRequest, and the Slice a PageResult. */
    @Override
    public PageResult<Order> findByCustomerId(String customerId, PageQuery page) {
        Slice<OrderJpaEntity> slice =
                jpa.findByCustomerId(customerId, PageRequest.of(page.page(), page.size(), NEWEST_FIRST));
        List<Order> orders = slice.getContent().stream().map(OrderPersistenceAdapter::toDomain).toList();
        return new PageResult<>(orders, page.page(), page.size(), slice.hasNext());
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
