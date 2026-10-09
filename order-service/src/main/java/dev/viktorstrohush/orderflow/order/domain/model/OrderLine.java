package dev.viktorstrohush.orderflow.order.domain.model;

import dev.viktorstrohush.orderflow.order.domain.exception.InvalidOrderException;
import dev.viktorstrohush.orderflow.order.domain.exception.OrderError;

import java.math.BigDecimal;
import java.util.Map;

public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {

    public static final int MAX_QUANTITY = 100;

    public OrderLine {
        if (sku == null || sku.isBlank()) {
            throw new InvalidOrderException(OrderError.SKU_REQUIRED, "The SKU is required");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException(OrderError.QUANTITY_NOT_POSITIVE,
                    "The quantity must be greater than 0 (SKU " + sku + ")", Map.of("sku", sku));
        }
        if (quantity > MAX_QUANTITY) {
            throw new InvalidOrderException(OrderError.QUANTITY_ABOVE_MAX,
                    "The maximum quantity per line is " + MAX_QUANTITY + " (SKU " + sku + ")",
                    Map.of("sku", sku, "max", MAX_QUANTITY));
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidOrderException(OrderError.NEGATIVE_PRICE,
                    "The unit price cannot be negative (SKU " + sku + ")", Map.of("sku", sku));
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
