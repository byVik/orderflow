package dev.viktorstrohush.orderflow.order.infrastructure.config;

import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.application.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionOperations;

/** Wires the application core to its adapters (the domain knows nothing about Spring). */
@Configuration
class ApplicationConfig {

    @Bean
    OrderService orderService(OrderRepository orders, OrderEventPublisher events, ProductCatalog catalog,
                              TransactionOperations tx) {
        return new OrderService(orders, events, catalog, tx);
    }
}
