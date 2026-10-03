package dev.viktorstrohush.orderflow.inventory.domain.model;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(UUID orderId, List<Item> items) {

    public record Item(String sku, int quantity) {
    }
}
