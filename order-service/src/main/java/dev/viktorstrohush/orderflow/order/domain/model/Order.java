package dev.viktorstrohush.orderflow.order.domain.model;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderStateException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderError;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raíz del pedido. Contiene las reglas de negocio y las transiciones de estado;
 * no depende de Spring ni de JPA.
 */
public final class Order {

    private final OrderId id;
    private final String customerId;
    private final List<OrderLine> lines;
    private final Instant createdAt;
    private OrderStatus status;
    private String rejectionReason;

    private Order(OrderId id, String customerId, List<OrderLine> lines, OrderStatus status,
                  String rejectionReason, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Crea un pedido nuevo en estado PENDING. */
    public static Order place(String customerId, List<OrderLine> lines) {
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidOrderException(OrderError.CUSTOMER_REQUIRED, "The customer is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException(OrderError.ORDER_EMPTY, "The order must have at least one line");
        }
        long distinctSkus = lines.stream().map(OrderLine::sku).distinct().count();
        if (distinctSkus != lines.size()) {
            throw new InvalidOrderException(OrderError.DUPLICATE_SKU, "An order cannot repeat a SKU");
        }
        return new Order(OrderId.newId(), customerId, lines, OrderStatus.PENDING, null, Instant.now());
    }

    /** Reconstruye un pedido desde persistencia. */
    public static Order restore(OrderId id, String customerId, List<OrderLine> lines, OrderStatus status,
                                String rejectionReason, Instant createdAt) {
        return new Order(id, customerId, lines, status, rejectionReason, createdAt);
    }

    public void confirm() {
        requireStatus(OrderStatus.PENDING, "confirm");
        this.status = OrderStatus.CONFIRMED;
    }

    public void reject(String reason) {
        requireStatus(OrderStatus.PENDING, "reject");
        this.status = OrderStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void cancel() {
        requireStatus(OrderStatus.PENDING, "cancel");
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public BigDecimal total() {
        return lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw new InvalidOrderStateException(
                    "Cannot " + action + " order " + id + " in status " + status, status);
        }
    }

    public OrderId id() { return id; }
    public String customerId() { return customerId; }
    public List<OrderLine> lines() { return lines; }
    public OrderStatus status() { return status; }
    public String rejectionReason() { return rejectionReason; }
    public Instant createdAt() { return createdAt; }
}
