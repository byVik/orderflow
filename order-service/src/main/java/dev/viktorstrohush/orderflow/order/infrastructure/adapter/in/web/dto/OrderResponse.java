package dev.viktorstrohush.orderflow.order.infrastructure.adapter.in.web.dto;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, String status, String rejectionReason, BigDecimal total,
                            Instant createdAt, List<Line> lines) {

    public record Line(String sku, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id().value(),
                order.status().name(),
                order.rejectionReason(),
                order.total(),
                order.createdAt(),
                order.lines().stream()
                        .map(l -> new Line(l.sku(), l.quantity(), l.unitPrice(), l.subtotal()))
                        .toList());
    }
}
