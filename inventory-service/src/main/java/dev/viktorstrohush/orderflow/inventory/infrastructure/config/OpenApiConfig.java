package dev.viktorstrohush.orderflow.inventory.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "OrderFlow · Inventory Service", version = "1.0",
        description = "Product catalog and stock reservation"))
class OpenApiConfig {
}
