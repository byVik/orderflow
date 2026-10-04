package dev.viktorstrohush.orderflow.order.domain.model;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;

import java.math.BigDecimal;

public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {

    public static final int MAX_QUANTITY = 100;

    public OrderLine {
        if (sku == null || sku.isBlank()) {
            throw new InvalidOrderException("El SKU es obligatorio");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("La cantidad debe ser mayor que 0 (SKU " + sku + ")");
        }
        if (quantity > MAX_QUANTITY) {
            throw new InvalidOrderException("La cantidad máxima por línea es " + MAX_QUANTITY + " (SKU " + sku + ")");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidOrderException("El precio unitario no puede ser negativo (SKU " + sku + ")");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
