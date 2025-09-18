package com.manage.crm.infrastructure.message.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Kafka 설정 클래스
 * Redis+Kafka 스케줄러가 활성화된 경우에만 Bean이 생성됩니다.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}") private val bootstrapServers: String,

    @Value("\${spring.kafka.consumer.group-id:crm-scheduler-group}") private val consumerGroupId: String,

    private val objectMapper: ObjectMapper
) {

    companion object {
        const val SCHEDULED_TASKS_TOPIC = "scheduled-tasks-execution"
    }

    // Producer Configuration
    @Bean
    fun producerConfigs(): Map<String, Any> = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        ProducerConfig.ACKS_CONFIG to "all", // 모든 replica에서 확인
        ProducerConfig.RETRIES_CONFIG to 3,
        ProducerConfig.BATCH_SIZE_CONFIG to 16384,
        ProducerConfig.LINGER_MS_CONFIG to 1,
        ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432
    )

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val factory = DefaultKafkaProducerFactory<String, Any>(producerConfigs())
        factory.setValueSerializer(JsonSerializer(objectMapper))
        return factory
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> = KafkaTemplate(producerFactory())

    // Consumer Configuration
    @Bean
    fun consumerConfigs(): Map<String, Any> = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to consumerGroupId,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false, // 수동 커밋
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 10,
        JsonDeserializer.TRUSTED_PACKAGES to "*" // 개발 환경용. 운영에서는 명시적 패키지 지정 권장
    )

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        return DefaultKafkaConsumerFactory(
            consumerConfigs(),
            StringDeserializer(),
            JsonDeserializer(Any::class.java, objectMapper)
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(3) // 동시 처리 스레드 수
        factory.containerProperties.isMissingTopicsFatal = false
        return factory
    }
}
