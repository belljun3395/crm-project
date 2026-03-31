package com.manage.crm.integration.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

/**
 * 간단한 TestContainer 설정 (docker-compose.yml 기준)
 */
object SimpleTestContainers {

    // Docker Compose와 동일한 이미지/설정 사용
    private val postgres by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("crm")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true)
            .apply { start() }
    }

    private val localstack by lazy {
        LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.SES)
            .withReuse(true)
            .apply { start() }
    }

    private val kafka by lazy {
        KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"))
            .withReuse(true)
            .apply { start() }
    }

    private val redis by lazy {
        GenericContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "password")
            .withReuse(true)
            .apply { start() }
    }

    fun register(registry: DynamicPropertyRegistry) {
        // PostgreSQL 설정
        registry.add("spring.r2dbc.url") { "r2dbc:pool:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/crm" }
        registry.add("spring.r2dbc.username") { postgres.username }
        registry.add("spring.r2dbc.password") { postgres.password }
        registry.add("spring.r2dbc.routing.master-url") { "r2dbc:pool:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/crm" }
        registry.add("spring.r2dbc.routing.replica-url") { "r2dbc:pool:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/crm" }
        registry.add("spring.r2dbc.routing.username") { postgres.username }
        registry.add("spring.r2dbc.routing.password") { postgres.password }
        registry.add("spring.datasource.jdbc-url") { postgres.jdbcUrl }
        registry.add("spring.datasource.username") { postgres.username }
        registry.add("spring.datasource.password") { postgres.password }
        registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }

        // LocalStack 설정
        registry.add("spring.aws.endpoint-url") { localstack.endpoint.toString() }
        registry.add("spring.aws.region") { localstack.region }
        registry.add("spring.aws.credentials.access-key") { localstack.accessKey }
        registry.add("spring.aws.credentials.secret-key") { localstack.secretKey }

        // Kafka 설정
        registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }

        // Redis 설정 (단일 노드로 간소화)
        registry.add("spring.data.redis.host") { redis.host }
        registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        registry.add("spring.data.redis.password") { "password" }
        registry.add("spring.data.redis.cluster.nodes") { listOf("${redis.host}:${redis.getMappedPort(6379)}") }
        registry.add("spring.data.redis.cluster.password") { "password" }
    }
}
