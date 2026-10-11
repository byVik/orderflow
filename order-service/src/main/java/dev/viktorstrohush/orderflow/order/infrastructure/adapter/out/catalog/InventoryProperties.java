package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * The {@code orderflow.inventory.*} block of application.yml as one typed object, instead of three
 * loose {@code @Value}. With {@code @Validated} Spring checks it while starting: a blank URL stops
 * the application there, not on the first order.
 */
@Validated
@ConfigurationProperties(prefix = "orderflow.inventory")
record InventoryProperties(@NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
}
