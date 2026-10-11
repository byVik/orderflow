package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.in.web.dto;

import dev.viktorstrohush.orderflow.inventory.domain.model.Product;

import java.math.BigDecimal;

public record ProductResponse(String sku, String name, BigDecimal price, int availableQuantity) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.sku(), product.name(), product.price(), product.availableQuantity());
    }
}
