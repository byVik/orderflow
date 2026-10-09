package dev.viktorstrohush.orderflow.order.application.service;

import dev.viktorstrohush.orderflow.order.application.port.in.ApplyStockResultUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.CancelOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.in.GetOrdersQuery;
import dev.viktorstrohush.orderflow.order.application.port.in.PlaceOrderUseCase;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderError;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderNotFoundException;
import dev.viktorstrohush.orderflow.order.domain.model.Order;
import dev.viktorstrohush.orderflow.order.domain.model.OrderId;
import dev.viktorstrohush.orderflow.order.domain.model.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

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
    private final TransactionOperations tx;

    public OrderService(OrderRepository orders, OrderEventPublisher events, ProductCatalog catalog,
                        TransactionOperations tx) {
        this.orders = orders;
        this.events = events;
        this.catalog = catalog;
        this.tx = tx;
    }

    /**
     * Sin @Transactional a propósito: los precios se piden por HTTP y esa llamada no debe retener
     * una conexión a la base de datos. La transacción cubre solo guardar el pedido y su evento.
     */
    @Override
    public Order place(PlaceOrderCommand command) {
        Order order = Order.place(command.customerId(), pricedLines(command));
        Order saved = tx.execute(status -> {
            Order persisted = orders.save(order);
            events.publishOrderPlaced(persisted);
            return persisted;
        });
        log.info("Pedido {} creado para el cliente {}", saved.id(), saved.customerId());
        return saved;
    }

    private List<OrderLine> pricedLines(PlaceOrderCommand command) {
        Set<String> skus = command.lines().stream()
                .map(PlaceOrderCommand.Line::sku)
                .collect(Collectors.toSet());
        Map<String, BigDecimal> prices = catalog.pricesFor(skus);
        return command.lines().stream()
                .map(l -> {
                    BigDecimal price = prices.get(l.sku());
                    if (price == null) {
                        throw new InvalidOrderException(OrderError.UNKNOWN_PRODUCT,
                                "Product " + l.sku() + " is not in the catalog", Map.of("sku", l.sku()));
                    }
                    return new OrderLine(l.sku(), l.quantity(), price);
                })
                .toList();
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
        Order saved = orders.save(order);
        // inventory-service devolverá el stock si ya lo había reservado (compensación de la saga).
        events.publishOrderCancelled(saved);
        return saved;
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
