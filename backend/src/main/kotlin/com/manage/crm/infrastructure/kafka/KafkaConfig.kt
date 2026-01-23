package com.manage.crm.infrastructure.kafka

import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Kafka configuration for scheduled task system
 * Configures producers and consumers for the scheduled-tasks topic
 */
@Configuration
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id:crm-scheduled-tasks-consumer}")
    private lateinit var groupId: String

    /**
     * Producer configuration for ScheduledTaskEvent
     */
    @Bean
    fun scheduledTaskProducerFactory(): ProducerFactory<String, ScheduledTaskEvent> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all", // Wait for all replicas
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1, // Ensure ordering
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true // Prevent duplicates
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun scheduledTaskKafkaTemplate(): KafkaTemplate<String, ScheduledTaskEvent> {
        return KafkaTemplate(scheduledTaskProducerFactory())
    }

    @Bean
    fun stringProducerFactory(): ProducerFactory<String, String> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(stringProducerFactory())
    }

    /**
     * Consumer configuration for ScheduledTaskEvent
     */
    @Bean
    fun scheduledTaskConsumerFactory(): ConsumerFactory<String, ScheduledTaskEvent> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false, // Manual acknowledgment
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 10,
            JsonDeserializer.TRUSTED_PACKAGES to "*", // Allow all packages for deserialization
            JsonDeserializer.VALUE_DEFAULT_TYPE to ScheduledTaskEvent::class.java.name
        )
        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            JsonDeserializer(ScheduledTaskEvent::class.java)
        )
    }

    @Bean
    fun scheduledTaskKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ScheduledTaskEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ScheduledTaskEvent>()
        factory.consumerFactory = scheduledTaskConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL // Manual acknowledgment
        factory.setConcurrency(3) // 3 concurrent consumers (matches 3 partitions)
        return factory
    }
}
