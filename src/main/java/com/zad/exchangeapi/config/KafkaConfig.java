package com.zad.exchangeapi.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
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

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.transaction}")
    private String transactionTopic;

    @Value("${app.kafka.topics.dlq}")
    private String dlqTopic;

    @Value("${app.kafka.retries.delay}")
    private long retryDelay;

    @Value("${app.kafka.topics.partitions:3}")
    private int partitions;

    @Value("${app.kafka.topics.replicas:1}")
    private short replicas;

    /**
     * Kafka admin client to manage topics.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Main unified transaction topic.
     */
    @Bean
    public NewTopic transactionTopic() {
        log.info("Creating transaction topic: {}", transactionTopic);
        return TopicBuilder.name(transactionTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    /**
     * Dead-letter queue for failed messages.
     */
    @Bean
    public NewTopic deadLetterTopic() {
        log.info("Creating dead-letter topic: {}", dlqTopic);
        return TopicBuilder.name(dlqTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    /**
     * Kafka listener container factory with error handling.
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
     * Error handler with DLQ and retry policy.
     */
    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

        DefaultErrorHandler errorHandler = getDefaultErrorHandler(kafkaTemplate);

        // Skip serialization errors immediately
        errorHandler.addNotRetryableExceptions(SerializationException.class);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for record: {}, due to: {}", deliveryAttempt, record, ex.getMessage())
        );

        return errorHandler;
    }

    private DefaultErrorHandler getDefaultErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    log.error("Sending message to DLQ. Topic: {}, Partition: {}, Exception: {}", record.topic(), record.partition(), ex.getMessage());
                    return new TopicPartition(dlqTopic, record.partition());
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(retryDelay, 3));
        return errorHandler;
    }
}


