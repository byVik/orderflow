package dev.viktorstrohush.orderflow.order.infrastructure.config;

import dev.viktorstrohush.orderflow.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class KafkaConfig {

    @Bean
    NewTopic orderPlacedTopic() {
        return TopicBuilder.name(Topics.ORDER_PLACED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic stockResultTopic() {
        return TopicBuilder.name(Topics.STOCK_RESULT).partitions(3).replicas(1).build();
    }

    /** 3 reintentos con 1 s de espera; si siguen fallando, el mensaje va a <topic>-dlt. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
    }
}
