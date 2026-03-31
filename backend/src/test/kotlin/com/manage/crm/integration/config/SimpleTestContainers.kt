package com.manage.crm.integration.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

object SimpleTestContainers {

    private const val REDIS_NODE_1_PORT = 16379
    private const val REDIS_NODE_2_PORT = 16380
    private const val REDIS_NODE_3_PORT = 16381
    private const val REDIS_PASSWORD = "password"

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

    /**
     * Redis cluster with 3 nodes running in a single container on fixed ports.
     * Fixed ports ensure cluster-announced addresses match external access addresses.
     */
    private val redis: GenericContainer<*> by lazy {
        val initScript = """
            redis-server --port $REDIS_NODE_1_PORT \
              --cluster-enabled yes --cluster-config-file /tmp/n1.conf \
              --cluster-node-timeout 5000 \
              --requirepass $REDIS_PASSWORD --masterauth $REDIS_PASSWORD \
              --loglevel warning --daemonize yes
            redis-server --port $REDIS_NODE_2_PORT \
              --cluster-enabled yes --cluster-config-file /tmp/n2.conf \
              --cluster-node-timeout 5000 \
              --requirepass $REDIS_PASSWORD --masterauth $REDIS_PASSWORD \
              --loglevel warning --daemonize yes
            redis-server --port $REDIS_NODE_3_PORT \
              --cluster-enabled yes --cluster-config-file /tmp/n3.conf \
              --cluster-node-timeout 5000 \
              --requirepass $REDIS_PASSWORD --masterauth $REDIS_PASSWORD \
              --loglevel warning --daemonize yes
            sleep 1
            echo yes | redis-cli -a $REDIS_PASSWORD \
              --cluster create \
              127.0.0.1:$REDIS_NODE_1_PORT \
              127.0.0.1:$REDIS_NODE_2_PORT \
              127.0.0.1:$REDIS_NODE_3_PORT \
              --cluster-replicas 0
            tail -f /dev/null
        """.trimIndent()

        val container = object : GenericContainer<Nothing>(DockerImageName.parse("redis:7.2-alpine")) {}
        container.withExposedPorts(REDIS_NODE_1_PORT, REDIS_NODE_2_PORT, REDIS_NODE_3_PORT)
        container.withCreateContainerCmdModifier { cmd ->
            val portBindings = com.github.dockerjava.api.model.Ports()
            listOf(REDIS_NODE_1_PORT, REDIS_NODE_2_PORT, REDIS_NODE_3_PORT).forEach { port ->
                portBindings.bind(
                    com.github.dockerjava.api.model.ExposedPort.tcp(port),
                    com.github.dockerjava.api.model.Ports.Binding.bindPort(port)
                )
            }
            cmd.hostConfig?.withPortBindings(portBindings)
        }
        container.withCommand("sh", "-c", initScript)
        container.waitingFor(Wait.forLogMessage(".*\\[OK\\] All 16384 slots covered.*", 1))
        container.withReuse(true)
        container.start()
        container
    }

    private val kafka by lazy {
        KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
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

        // Redis cluster 설정 (고정 포트 3-node 클러스터)
        redis // ensure container is started
        registry.add("spring.data.redis.cluster.nodes") {
            "localhost:$REDIS_NODE_1_PORT,localhost:$REDIS_NODE_2_PORT,localhost:$REDIS_NODE_3_PORT"
        }
        registry.add("spring.data.redis.cluster.connect-ip") { "localhost" }
        registry.add("spring.data.redis.cluster.password") { REDIS_PASSWORD }
        registry.add("spring.data.redis.cluster.max-redirects") { "3" }

        // Kafka 설정
        registry.add("spring.kafka.bootstrap-servers") { kafka.bootstrapServers }
        registry.add("scheduler.provider") { "redis-kafka" }
    }
}
