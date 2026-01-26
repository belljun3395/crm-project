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
 * Kafka configuration for scheduled task processing.
 * Only activated when using Redis+Kafka scheduler provider.
 */
@Configuration
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:29092}")
    private lateinit var bootstrapServers: String

    // ----------------- String Producer/Consumer (General Purpose) -----------------

    @Bean
    fun stringProducerFactory(): ProducerFactory<String, String> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun stringKafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(stringProducerFactory())
    }

    @Bean
    fun stringConsumerFactory(): ConsumerFactory<String, String> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            consumerFactory = stringConsumerFactory()
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }
    }

    // ----------------- Scheduled Task Producer/Consumer -----------------

    @Bean
    fun scheduledTaskProducerFactory(): ProducerFactory<String, ScheduledTaskEvent> {
        val config = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        )
        return DefaultKafkaProducerFactory(config)
    }

    @Bean
    fun scheduledTaskKafkaTemplate(): KafkaTemplate<String, ScheduledTaskEvent> {
        return KafkaTemplate(scheduledTaskProducerFactory())
    }

    @Bean
    fun scheduledTaskConsumerFactory(): ConsumerFactory<String, ScheduledTaskEvent> {
        val config = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.GROUP_ID_CONFIG to "crm-scheduled-tasks-consumer",
            JsonDeserializer.TRUSTED_PACKAGES to "*"
        )
        return DefaultKafkaConsumerFactory(
            config,
            StringDeserializer(),
            JsonDeserializer(ScheduledTaskEvent::class.java).apply {
                setRemoveTypeHeaders(false)
                addTrustedPackages("*")
                setUseTypeMapperForKey(true)
            }
        )
    }

    @Bean
    fun scheduledTaskKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ScheduledTaskEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, ScheduledTaskEvent>().apply {
            consumerFactory = scheduledTaskConsumerFactory()
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
            setConcurrency(3)
        }
    }
}
