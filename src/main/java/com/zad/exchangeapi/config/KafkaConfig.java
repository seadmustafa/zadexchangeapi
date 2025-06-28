package com.zad.exchangeapi.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topic configuration for deposit and withdraw messaging.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.deposit}")
    private String depositTopic;

    @Value("${app.kafka.topics.withdraw}")
    private String withdrawTopic;

    @Value("${app.kafka.topics.dlq}")
    private String dlqTopic;

    @Value("${app.kafka.retries.delay}")
    private long retryDelay;


    /**
     * Kafka admin client to manage topics.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic depositTopic() {
        return buildTopic(depositTopic);
    }

    @Bean
    public NewTopic withdrawTopic() {
        return buildTopic(withdrawTopic);
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return buildTopic(dlqTopic);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Kafka listener factory with DLQ and retry mechanism.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));

        return factory;
    }

    /**
     * Error handler that retries and sends failed messages to DLQ.
     */
    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(dlqTopic, record.partition())
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryDelay, 3));
    }
}

