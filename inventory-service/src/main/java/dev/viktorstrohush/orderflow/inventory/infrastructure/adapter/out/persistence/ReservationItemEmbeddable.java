package dev.viktorstrohush.orderflow.inventory.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Una línea de la reserva: fila de stock_reservation_items. */
@Embeddable
record ReservationItemEmbeddable(@Column(nullable = false) String sku, @Column(nullable = false) int quantity) {
}
