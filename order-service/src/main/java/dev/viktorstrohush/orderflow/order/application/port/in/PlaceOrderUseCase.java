package dev.viktorstrohush.orderflow.order.application.port.in;

import dev.viktorstrohush.orderflow.order.domain.model.Order;

import java.util.List;

public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);

    /** The price does not travel in the command: it always comes from the catalog. */
    record PlaceOrderCommand(String customerId, List<Line> lines) {
        public record Line(String sku, int quantity) {
        }
    }
}
