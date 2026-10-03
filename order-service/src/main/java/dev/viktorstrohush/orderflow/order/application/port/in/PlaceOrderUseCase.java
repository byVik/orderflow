package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

import java.util.List;

public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);

    /** El precio no viaja en el comando: se obtiene siempre del catálogo. */
    record PlaceOrderCommand(String customerId, List<Line> lines) {
        public record Line(String sku, int quantity) {
        }
    }
}
