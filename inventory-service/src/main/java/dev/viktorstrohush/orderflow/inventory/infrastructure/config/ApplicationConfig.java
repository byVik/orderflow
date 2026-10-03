package dev.viktorstrohush.orderflow.inventory.infrastructure.config;

import dev.viktorstrohush.orderflow.inventory.application.port.out.ProductRepository;
import dev.viktorstrohush.orderflow.inventory.application.port.out.ReservationLog;
import dev.viktorstrohush.orderflow.inventory.application.port.out.StockEventPublisher;
import dev.viktorstrohush.orderflow.inventory.application.service.InventoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    InventoryService inventoryService(ProductRepository products, ReservationLog reservations,
                                      StockEventPublisher events) {
        return new InventoryService(products, reservations, events);
    }
}
