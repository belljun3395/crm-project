package com.manage.crm.integration.config

import org.springframework.test.context.DynamicPropertyRegistry

data class TestAwsConfig(
    val region: String,
    val accessKey: String,
    val secretKey: String,
    val ses: TestSesConfig,
    val scheduler: TestSchedulerConfig
)

data class TestSesConfig(
    val verifiedEmails: List<String>,
    val configurationSet: String,
    val setupRetryCount: Int,
    val readyWaitRetryCount: Int
)

data class TestSchedulerConfig(
    val roleArn: String,
    val sqsArn: String,
    val groupName: String,
    val setupRetryCount: Int
)

data class TestRetryConfig(
    val maxRetries: Int,
    val initialDelayMs: Long,
    val exponentialBackoff: Boolean
)

object TestInfraSupport {
    private const val redisServiceName = "crm-redis-cluster"
    private const val localStackServiceName = "crm-localstack"
    private const val kafkaServiceName = "crm-kafka"
    private const val defaultRedisPassword = "password"

    private val localStackService = ComposeTestInfraConfig.service(localStackServiceName)
    private val kafkaService = ComposeTestInfraConfig.service(kafkaServiceName)
    private val redisService = ComposeTestInfraConfig.service(redisServiceName)

    val awsConfig = TestAwsConfig(
        region = localStackService.environment["AWS_DEFAULT_REGION"] ?: "ap-northeast-2",
        accessKey = localStackService.environment["AWS_ACCESS_KEY_ID"] ?: "test",
        secretKey = localStackService.environment["AWS_SECRET_ACCESS_KEY"] ?: "test",
        ses = TestSesConfig(
            verifiedEmails = listOf(
                "test@example.com",
                "notification@example.com",
                "noreply@example.com",
                "admin@example.com",
                "example@example.com"
            ),
            configurationSet = "test-configuration-set",
            setupRetryCount = 3,
            readyWaitRetryCount = 5
        ),
        scheduler = TestSchedulerConfig(
            roleArn = "arn:aws:iam::000000000000:role/TestRole",
            sqsArn = "arn:aws:sqs:ap-northeast-2:000000000000:test-queue",
            groupName = "test-group",
            setupRetryCount = 3
        )
    )

    val retryConfig = TestRetryConfig(
        maxRetries = 3,
        initialDelayMs = 500,
        exponentialBackoff = true
    )

    val useLocalStack: Boolean
        get() = System.getProperty("useLocalStack", "true").toBoolean()

    val localStackEndpoint: String
        get() = "http://localhost:${ComposeTestInfraConfig.hostPort(localStackServiceName, 4566)}"

    private val kafkaBootstrapServers: String
        get() = "localhost:${ComposeTestInfraConfig.hostPort(kafkaServiceName, 9092)}"

    private val redisNodes: List<String>
        get() {
            val nodes = mutableListOf<String>()
            for (containerPort in 7001..7006) {
                val hostPort = redisService.ports[containerPort] ?: continue
                nodes += "localhost:$hostPort"
            }
            return nodes
        }

    private val redisPassword: String
        get() {
            val joinedCommand = redisService.command.joinToString(" ")
            val match = Regex("/tmp/redis\\.sh\\s+(\\S+)\\s+\\d+")
                .findAll(joinedCommand)
                .lastOrNull()
            return match?.groupValues?.getOrNull(1) ?: defaultRedisPassword
        }

    fun register(registry: DynamicPropertyRegistry) {
        PostgresContainerSupport.register(registry)
        registry.add("spring.aws.endpoint-url") { localStackEndpoint }
        registry.add("spring.aws.region") { awsConfig.region }
        registry.add("spring.aws.credentials.access-key") { awsConfig.accessKey }
        registry.add("spring.aws.credentials.secret-key") { awsConfig.secretKey }
        registry.add("spring.kafka.bootstrap-servers") { kafkaBootstrapServers }
        registry.add("spring.data.redis.cluster.connect-ip") { "localhost" }
        registry.add("spring.data.redis.cluster.password") { redisPassword }
        registry.add("spring.data.redis.cluster.nodes") { redisNodes }
    }
}
