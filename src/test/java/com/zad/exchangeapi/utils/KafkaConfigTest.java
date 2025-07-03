package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.config.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @Mock
    private ConsumerFactory<String, Object> consumerFactory;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        kafkaConfig = new KafkaConfig();

        setPrivateField(kafkaConfig, "bootstrapServers", "localhost:9092");
        setPrivateField(kafkaConfig, "transactionTopic", "transaction-topic");
        setPrivateField(kafkaConfig, "dlqTopic", "dlq-topic");
        setPrivateField(kafkaConfig, "retryDelay", 1000L);
        setPrivateField(kafkaConfig, "partitions", 3);
        setPrivateField(kafkaConfig, "replicas", (short) 2);

        when(kafkaTemplate.isTransactional()).thenReturn(false);
    }

    @Test
    void kafkaAdmin_shouldContainBootstrapServers() {
        KafkaAdmin admin = kafkaConfig.kafkaAdmin();
        Map<String, Object> configs = admin.getConfigurationProperties();

        assertThat(configs)
                .containsEntry(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    }

    @Test
    void transactionTopic_shouldBeCreatedWithCorrectSettings() {
        NewTopic topic = kafkaConfig.transactionTopic();

        assertThat(topic.name()).isEqualTo("transaction-topic");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 2);
    }

    @Test
    void deadLetterTopic_shouldBeCreatedWithCorrectSettings() {
        NewTopic topic = kafkaConfig.deadLetterTopic();

        assertThat(topic.name()).isEqualTo("dlq-topic");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 2);
    }

    @Test
    void kafkaListenerContainerFactory_shouldCreateFactoryWithInjectedDependencies() {
        var factory = kafkaConfig.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate);

        assertThat(factory).isNotNull();
    }


    @Test
    void defaultErrorHandler_shouldRegisterRetryListener() {
        DefaultErrorHandler handler = (DefaultErrorHandler)
                kafkaConfig.commonErrorHandler(kafkaTemplate);

        assertThat(handler).isNotNull();

        // Simulate setting retry listeners to confirm no exceptions
        handler.setRetryListeners((record, ex, attempt) -> {
            assertThat(record).isNotNull();
            assertThat(ex).isNotNull();
            assertThat(attempt).isGreaterThanOrEqualTo(1);
        });
    }

    // Helper methods for reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
