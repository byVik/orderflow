package dev.viktorstrohush.orderflow.order.domain.model;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;

import java.math.BigDecimal;

public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {

    public OrderLine {
        if (sku == null || sku.isBlank()) {
            throw new InvalidOrderException("El SKU es obligatorio");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("La cantidad debe ser mayor que 0 (SKU " + sku + ")");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidOrderException("El precio unitario no puede ser negativo (SKU " + sku + ")");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
