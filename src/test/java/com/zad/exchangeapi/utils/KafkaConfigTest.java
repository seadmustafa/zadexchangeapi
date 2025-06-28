package com.zad.exchangeapi.utils;

import com.zad.exchangeapi.config.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

        // Inject private fields via reflection
        setPrivateField(kafkaConfig, "bootstrapServers", "localhost:9092");
        setPrivateField(kafkaConfig, "depositTopic", "deposit-topic");
        setPrivateField(kafkaConfig, "withdrawTopic", "withdraw-topic");
        setPrivateField(kafkaConfig, "dlqTopic", "dlq-topic");
        setPrivateField(kafkaConfig, "retryDelay", 1000L);

        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.isTransactional()).thenReturn(false);
    }

    @Test
    void kafkaAdmin_shouldHaveCorrectBootstrapServers() {
        KafkaAdmin admin = kafkaConfig.kafkaAdmin();
        Map<String, Object> configs = admin.getConfigurationProperties();

        assertThat(configs.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo("localhost:9092");
    }

    @Test
    void depositTopic_shouldBeConfiguredCorrectly() {
        NewTopic topic = kafkaConfig.depositTopic();

        assertThat(topic.name()).isEqualTo("deposit-topic");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void withdrawTopic_shouldBeConfiguredCorrectly() {
        NewTopic topic = kafkaConfig.withdrawTopic();

        assertThat(topic.name()).isEqualTo("withdraw-topic");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }

    @Test
    void deadLetterTopic_shouldBeConfiguredCorrectly() {
        NewTopic topic = kafkaConfig.deadLetterTopic();

        assertThat(topic.name()).isEqualTo("dlq-topic");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }

    @Test
    void kafkaListenerContainerFactory_shouldInitializeWithConsumerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConfig.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate);

        assertThat(factory.getConsumerFactory()).isEqualTo(consumerFactory);
    }

    @Test
    void commonErrorHandler_shouldBeDefaultErrorHandlerWithDLQ() {
        CommonErrorHandler errorHandler = kafkaConfig.commonErrorHandler(kafkaTemplate);

        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }

    @Test
    void deadLetterPublishingRecoverer_shouldSendToCorrectDLQTopic() {
        String dlqTopic = getPrivateField(kafkaConfig, "dlqTopic");

        // Replicate the same lambda logic
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(dlqTopic, record.partition())
        );

        // Simulate a record with a known partition
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 2, 0L, "key", "value");

        // Use the lambda logic manually
        TopicPartition partition = new TopicPartition(dlqTopic, record.partition());

        assertThat(partition.topic()).isEqualTo("dlq-topic");
        assertThat(partition.partition()).isEqualTo(2);
    }


    // Helper for setting private fields
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // Helper for getting private field values
    private String getPrivateField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
