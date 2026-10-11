package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** ApplicationContextRunner starts a tiny context per test: enough to see what Spring does with the properties. */
class InventoryPropertiesTest {

    @EnableConfigurationProperties(InventoryProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(Config.class)
            .withPropertyValues(
                    "orderflow.inventory.connect-timeout=2s",
                    "orderflow.inventory.read-timeout=3s");

    @Test
    void bindsTheBlockOfPropertiesToOneTypedObject() {
        context.withPropertyValues("orderflow.inventory.base-url=http://inventory-service:8082")
                .run(started -> assertThat(started.getBean(InventoryProperties.class)).isEqualTo(
                        new InventoryProperties("http://inventory-service:8082", Duration.ofSeconds(2), Duration.ofSeconds(3))));
    }

    @Test
    void aBlankUrlStopsTheStartup() {
        context.withPropertyValues("orderflow.inventory.base-url=")
                .run(started -> assertThat(started).getFailure()
                        .hasRootCauseInstanceOf(BindValidationException.class)
                        .rootCause().hasMessageContaining("baseUrl"));
    }
}
