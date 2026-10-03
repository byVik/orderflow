package dev.viktorstrohush.orderflow.order.infrastructure.config;

import dev.viktorstrohush.orderflow.order.application.port.out.OrderEventPublisher;
import dev.viktorstrohush.orderflow.order.application.port.out.OrderRepository;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import dev.viktorstrohush.orderflow.order.application.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla el núcleo de la aplicación con sus adaptadores (el dominio no conoce Spring). */
@Configuration
class ApplicationConfig {

    @Bean
    OrderService orderService(OrderRepository orders, OrderEventPublisher events, ProductCatalog catalog) {
        return new OrderService(orders, events, catalog);
    }
}
