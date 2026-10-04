package dev.viktorstrohush.orderflow.inventory.infrastructure.config;

import dev.viktorstrohush.orderflow.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/** Topics, serialización y gestión de errores de Kafka. Activa el planificador del relay del outbox. */
@Configuration
@EnableScheduling
class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    NewTopic orderPlacedTopic() {
        return TopicBuilder.name(Topics.ORDER_PLACED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderCancelledTopic() {
        return TopicBuilder.name(Topics.ORDER_CANCELLED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic stockResultTopic() {
        return TopicBuilder.name(Topics.STOCK_RESULT).partitions(3).replicas(1).build();
    }

    /** Mismas particiones que el original: el recoverer envía cada mensaje a la misma partición. */
    @Bean
    NewTopic orderPlacedDeadLetterTopic() {
        return TopicBuilder.name(Topics.ORDER_PLACED + Topics.DLT_SUFFIX).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderCancelledDeadLetterTopic() {
        return TopicBuilder.name(Topics.ORDER_CANCELLED + Topics.DLT_SUFFIX).partitions(3).replicas(1).build();
    }

    /**
     * El serializador de valores se elige por tipo: el relay del outbox envía JSON ya serializado
     * (String), el dead-letter topic puede recibir los bytes originales de un mensaje ilegible, y
     * el resto de objetos se convierten a JSON. El orden importa: del tipo más concreto al más general.
     */
    @Bean
    DefaultKafkaProducerFactoryCustomizer valueSerializerByType() {
        return factory -> {
            Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
            delegates.put(byte[].class, new ByteArraySerializer());
            delegates.put(String.class, new StringSerializer());
            delegates.put(Object.class, new JsonSerializer<>());
            withObjectValues(factory).setValueSerializer(new DelegatingByTypeSerializer(delegates, true));
        };
    }

    @SuppressWarnings("unchecked")
    private static DefaultKafkaProducerFactory<Object, Object> withObjectValues(DefaultKafkaProducerFactory<?, ?> factory) {
        return (DefaultKafkaProducerFactory<Object, Object>) factory;
    }

    /** 3 reintentos con 1 s de espera; si siguen fallando, el mensaje va a <topic>-dlt y se deja constancia. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> template) {
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("Intento {} fallido para {}-{}@{}: {}", deliveryAttempt, record.topic(),
                        record.partition(), record.offset(), ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("Mensaje con clave {} enviado al dead-letter topic {}{}", record.key(),
                        record.topic(), Topics.DLT_SUFFIX, ex);
            }
        });
        return handler;
    }
}
