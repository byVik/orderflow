package dev.viktorstrohush.orderflow.order.infrastructure.config;

import dev.viktorstrohush.orderflow.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
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

/** Kafka topics, serialization and error handling. Enables the scheduler for the outbox relay. */
@Configuration
@EnableScheduling
@Slf4j
class KafkaConfig {

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

    /** Same partitions as the original: the recoverer sends each message to the same partition. */
    @Bean
    NewTopic stockResultDeadLetterTopic() {
        return TopicBuilder.name(Topics.STOCK_RESULT + Topics.DLT_SUFFIX).partitions(3).replicas(1).build();
    }

    /**
     * The value serializer is chosen by type: the outbox relay sends already serialized JSON
     * (String), the dead-letter topic may receive the original bytes of an unreadable message, and
     * all other objects are converted to JSON. Order matters: from the most specific type to the most general.
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

    /** 3 retries with a 1 s wait; if they keep failing, the message goes to <topic>-dlt and it is logged. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> template) {
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
        handler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                log.warn("Attempt {} failed for {}-{}@{}: {}", deliveryAttempt, record.topic(),
                        record.partition(), record.offset(), ex.getMessage());
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                log.error("Message with key {} sent to dead-letter topic {}{}", record.key(),
                        record.topic(), Topics.DLT_SUFFIX, ex);
            }
        });
        return handler;
    }
}
