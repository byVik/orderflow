package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One line of the reservation: a row of stock_reservation_items. */
@Embeddable
record ReservationItemEmbeddable(@Column(nullable = false) String sku, @Column(nullable = false) int quantity) {
}
