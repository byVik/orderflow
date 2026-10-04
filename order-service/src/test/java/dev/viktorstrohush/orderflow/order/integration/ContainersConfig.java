package dev.viktorstrohush.orderflow.order.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * PostgreSQL y Kafka reales para los tests de integración. Al declararlos como beans, su ciclo de
 * vida va unido al del contexto de Spring: arrancan antes que el DataSource y se paran después.
 * Con contenedores estáticos de JUnit se paraban antes y el relay del outbox seguía consultando
 * una base de datos que ya no existía.
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
