package com.manage.crm.config

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration

/**
 * Shared testcontainers initializer for Redis and Kafka.
 * Can be used by both integration tests and module tests.
 */
class RedisKafkaContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        private val logger = LoggerFactory.getLogger(RedisKafkaContainerInitializer::class.java)

        // Lazy initialization to reuse containers across test classes
        val redisContainer: GenericContainer<*> by lazy {
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true)
                .withStartupTimeout(Duration.ofMinutes(2))
                .waitingFor(Wait.forListeningPort())
        }

        val kafkaContainer: KafkaContainer by lazy {
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                .withReuse(true)
                .withStartupTimeout(Duration.ofMinutes(2))
        }

        private var initialized = false

        @Synchronized
        fun ensureContainersStarted() {
            if (!initialized) {
                logger.info("Starting Redis and Kafka containers...")
                Startables.deepStart(redisContainer, kafkaContainer).join()
                logger.info("Redis container started at {}:{}", redisContainer.host, redisContainer.getMappedPort(6379))
                logger.info("Kafka container started at {}", kafkaContainer.bootstrapServers)
                initialized = true
            }
        }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        ensureContainersStarted()

        val redisHost = redisContainer.host
        val redisPort = redisContainer.getMappedPort(6379)
        val bootstrapServers = kafkaContainer.bootstrapServers

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "spring.data.redis.host=$redisHost",
            "spring.data.redis.port=$redisPort",
            // Clear cluster nodes to force standalone mode
            "spring.data.redis.cluster.nodes=",
            "spring.kafka.bootstrap-servers=$bootstrapServers"
        )

        logger.info("Configured Redis at {}:{} and Kafka at {}", redisHost, redisPort, bootstrapServers)
    }
}
