package dev.viktorstrohush.orderflow.order.application.service;

import dev.viktorstrohush.orderflow.order.application.port.in.ApplyStockResultUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Casos de uso de pedidos. Solo depende de los puertos; Spring se usa únicamente para
 * la demarcación transaccional.
 */
public class OrderService implements PlaceOrderUseCase, GetOrdersQuery, CancelOrderUseCase, ApplyStockResultUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final OrderEventPublisher events;
    private final ProductCatalog catalog;

    public OrderService(OrderRepository orders, OrderEventPublisher events, ProductCatalog catalog) {
        this.orders = orders;
        this.events = events;
        this.catalog = catalog;
    }

    @Override
    @Transactional
    public Order place(PlaceOrderCommand command) {
        Set<String> skus = command.lines().stream()
                .map(PlaceOrderCommand.Line::sku)
                .collect(Collectors.toSet());
        Map<String, BigDecimal> prices = catalog.pricesFor(skus);
        List<OrderLine> lines = command.lines().stream()
                .map(l -> {
                    BigDecimal price = prices.get(l.sku());
                    if (price == null) {
                        throw new InvalidOrderException("El producto " + l.sku() + " no existe en el catálogo");
                    }
                    return new OrderLine(l.sku(), l.quantity(), price);
                })
                .toList();
        Order saved = orders.save(Order.place(command.customerId(), lines));
        events.publishOrderPlaced(saved);
        log.info("Pedido {} creado para el cliente {}", saved.id(), saved.customerId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(OrderId id, String customerId) {
        return orders.findById(id)
                .filter(o -> o.customerId().equals(customerId))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> listByCustomer(String customerId) {
        return orders.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public Order cancel(OrderId id, String customerId) {
        Order order = getById(id, customerId);
        order.cancel();
        return orders.save(order);
    }

    @Override
    @Transactional
    public void apply(OrderId id, boolean reserved, String reason) {
        Order order = orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        if (!order.isPending()) {
            log.info("Resultado de stock ignorado para el pedido {} en estado {}", id, order.status());
            return;
        }
        if (reserved) {
            order.confirm();
        } else {
            order.reject(reason);
        }
        orders.save(order);
        log.info("Pedido {} -> {}", id, order.status());
    }
}
