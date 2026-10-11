package dev.viktorstrohush.orderflow.inventory.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Real PostgreSQL and Kafka for the integration tests. Declared as beans, their lifecycle is
 * tied to that of the Spring context: they start before the DataSource and stop after it.
 * With static JUnit containers they stopped earlier and the outbox relay kept querying
 * a database that no longer existed.
 */
@TestConfiguration(proxyBeanMethods = false)
class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer("apache/kafka-native:3.8.0");
    }
}
