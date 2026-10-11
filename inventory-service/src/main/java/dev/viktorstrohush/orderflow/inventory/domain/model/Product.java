package dev.viktorstrohush.orderflow.inventory.domain.model;

import dev.viktorstrohush.orderflow.inventory.domain.exception.InsufficientStockException;

import java.math.BigDecimal;
import java.util.Objects;

public final class Product {

    private final String sku;
    private final String name;
    private final BigDecimal price;
    private int availableQuantity;

    public Product(String sku, String name, BigDecimal price, int availableQuantity) {
        this.sku = Objects.requireNonNull(sku);
        this.name = Objects.requireNonNull(name);
        this.price = Objects.requireNonNull(price);
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.availableQuantity = availableQuantity;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && quantity <= availableQuantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(sku, quantity, availableQuantity);
        }
        availableQuantity -= quantity;
    }

    /** Returns to stock units that had been reserved (an order's cancellation). */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("The quantity to release must be greater than 0");
        }
        availableQuantity += quantity;
    }

    public String sku() { return sku; }
    public String name() { return name; }
    public BigDecimal price() { return price; }
    public int availableQuantity() { return availableQuantity; }
}
