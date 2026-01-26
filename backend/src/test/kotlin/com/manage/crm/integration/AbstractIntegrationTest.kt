package com.manage.crm.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.ConfigurationSet
import com.amazonaws.services.simpleemail.model.CreateConfigurationSetRequest
import com.amazonaws.services.simpleemail.model.VerifyEmailIdentityRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.integration.config.TestContainerConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = ["message.provider=redis-kafka", "scheduler.provider=redis-kafka", "mail.provider=javamail"])
abstract class AbstractIntegrationTest : DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    @LocalServerPort
    private var port: Int = 0

    protected val objectMapper: ObjectMapper = ObjectMapper()
    protected lateinit var webTestClient: WebTestClient

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        beforeSpec {
            webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:$port")
                .entityExchangeResultConsumer {
                    val sb = StringBuffer()
                    sb.appendLine()
                    sb.appendLine("================= HTTP Exchange Result ================")
                    sb.appendLine("Request: ${it.method} ${it.url}")
                    sb.appendLine("Request Headers: ${it.requestHeaders}")
                    sb.appendLine("Request Body: ${it.requestBodyContent?.toString(Charset.defaultCharset())}")
                    sb.appendLine()
                    sb.appendLine("Response Status: ${it.status}")
                    sb.appendLine("Response Headers: ${it.responseHeaders}")
                    sb.appendLine("Response Body: ${it.responseBodyContent?.toString(Charset.defaultCharset())}")
                    sb.appendLine("========================================================")
                    logger.info(sb.toString())
                }
                .build()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractIntegrationTest::class.java)

        // Load configuration from YAML
        private val config = TestContainerConfig.load()
        private val mysqlConfig = config.testcontainers.mysql
        private val localStackConfig = config.testcontainers.localstack
        private val awsConfig = config.testcontainers.aws
        private val retryConfig = config.testcontainers.retry

        private val useLocalStack = System.getProperty("useLocalStack", localStackConfig.enabled.toString()).toBoolean()

        // Shared network for container communication
        @JvmStatic
        private val sharedNetwork: Network = Network.newNetwork()

        // ==================== MySQL Container ====================
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> = MySQLContainer(mysqlConfig.image)
            .withDatabaseName(mysqlConfig.databaseName)
            .withUsername(mysqlConfig.username)
            .withPassword(mysqlConfig.password)
            .withNetwork(sharedNetwork)
            .withNetworkAliases(mysqlConfig.networkAlias)
            .withReuse(mysqlConfig.reuse)
            .withStartupTimeout(mysqlConfig.getStartupTimeout())
            .waitingFor(
                Wait.forLogMessage(mysqlConfig.readyLogPattern, mysqlConfig.readyLogCount)
                    .withStartupTimeout(mysqlConfig.getStartupTimeout())
            )

        // ==================== Redis Container (for redis-kafka scheduler) ====================
        @Container
        @JvmStatic
        val redisContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withNetwork(sharedNetwork)
            .withNetworkAliases("redis")
            .withReuse(true)
            .withStartupTimeout(Duration.ofMinutes(2))
            .waitingFor(Wait.forListeningPort())

        // ==================== Kafka Container (for redis-kafka scheduler) ====================
        @Container
        @JvmStatic
        val kafkaContainer: KafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(sharedNetwork)
            .withNetworkAliases("kafka")
            .withReuse(true)
            .withStartupTimeout(Duration.ofMinutes(2))

        // ==================== LocalStack Container (optional, for AWS services) ====================
        @Container
        @JvmStatic
        val localStackContainer: GenericContainer<*>? = if (useLocalStack) {
            GenericContainer(localStackConfig.image)
                .withExposedPorts(*localStackConfig.ports.toTypedArray())
                .withNetwork(sharedNetwork)
                .withNetworkAliases(localStackConfig.networkAlias)
                .withReuse(localStackConfig.reuse)
                .withStartupTimeout(localStackConfig.getStartupTimeout())
                .withEnv(localStackConfig.environment)
                .waitingFor(
                    Wait.forLogMessage(localStackConfig.readyLogPattern, localStackConfig.readyLogCount)
                        .withStartupTimeout(localStackConfig.getStartupTimeout())
                )
        } else {
            null
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Start all containers in parallel for better performance
            val containersToStart = mutableListOf(mysqlContainer, redisContainer, kafkaContainer)
            if (useLocalStack && localStackContainer != null) {
                containersToStart.add(localStackContainer)
            }
            logger.info("Starting test containers: MySQL, Redis, Kafka" + if (useLocalStack) ", LocalStack" else "")
            Startables.deepStart(*containersToStart.toTypedArray()).join()

            // Database configuration
            configureDatabaseProperties(registry)

            // Redis configuration for redis-kafka provider
            configureRedisProperties(registry)

            // Kafka configuration for redis-kafka provider
            configureKafkaProperties(registry)

            // LocalStack configuration (only if enabled)
            if (useLocalStack && localStackContainer != null) {
                configureLocalStackProperties(registry)
                setupAwsServices()
            }
        }

        private fun configureDatabaseProperties(registry: DynamicPropertyRegistry) {
            val jdbcUrl = mysqlContainer.jdbcUrl
            val r2dbcUrl = "r2dbc:pool:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(3306)}/${mysqlConfig.databaseName}?useSSL=false"

            logger.info("MySQL Container started. JDBC URL: $jdbcUrl, R2DBC URL: $r2dbcUrl")

            registry.add("spring.r2dbc.url") { r2dbcUrl }
            registry.add("spring.r2dbc.username") { mysqlConfig.username }
            registry.add("spring.r2dbc.password") { mysqlConfig.password }

            // R2DBC routing configuration
            registry.add("spring.r2dbc.routing.master-url") {
                "r2dbc:pool:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(3306)}/${mysqlConfig.databaseName}"
            }
            registry.add("spring.r2dbc.routing.replica-url") {
                "r2dbc:pool:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(3306)}/${mysqlConfig.databaseName}"
            }
            registry.add("spring.r2dbc.routing.username") { mysqlConfig.username }
            registry.add("spring.r2dbc.routing.password") { mysqlConfig.password }

            registry.add("spring.flyway.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.flyway.user") { mysqlConfig.username }
            registry.add("spring.flyway.password") { mysqlConfig.password }
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlConfig.username }
            registry.add("spring.datasource.password") { mysqlConfig.password }
        }

        private fun configureRedisProperties(registry: DynamicPropertyRegistry) {
            val redisHost = redisContainer.host
            val redisPort = redisContainer.getMappedPort(6379)
            logger.info("Redis Container started. Host: $redisHost, Port: $redisPort")

            // Use standalone Redis for tests (not cluster)
            registry.add("spring.data.redis.host") { redisHost }
            registry.add("spring.data.redis.port") { redisPort }
            // Clear cluster nodes to force standalone mode
            registry.add("spring.data.redis.cluster.nodes") { "" }
        }

        private fun configureKafkaProperties(registry: DynamicPropertyRegistry) {
            val bootstrapServers = kafkaContainer.bootstrapServers
            logger.info("Kafka Container started. Bootstrap servers: $bootstrapServers")

            registry.add("spring.kafka.bootstrap-servers") { bootstrapServers }
        }

        private fun configureLocalStackProperties(registry: DynamicPropertyRegistry) {
            val localStackEndpoint = "http://${localStackContainer!!.host}:${localStackContainer.getMappedPort(4566)}"

            registry.add("spring.aws.endpoint-url") { localStackEndpoint }
            registry.add("spring.aws.credentials.access-key") { awsConfig.accessKey }
            registry.add("spring.aws.credentials.secret-key") { awsConfig.secretKey }
            registry.add("spring.aws.region") { awsConfig.region }
            registry.add("spring.aws.mail.configuration-set.default") { awsConfig.ses.configurationSet }
            registry.add("spring.mail.username") { awsConfig.ses.verifiedEmails.firstOrNull() ?: "example@example.com" }
            registry.add("spring.aws.schedule.role-arn") { awsConfig.scheduler.roleArn }
            registry.add("spring.aws.schedule.sqs-arn") { awsConfig.scheduler.sqsArn }
            registry.add("spring.aws.schedule.group-name") { awsConfig.scheduler.groupName }
        }

        private fun setupAwsServices() {
            if (localStackContainer != null) {
                val localStackEndpoint = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(4566)}"

                // Run AWS services setup in parallel
                val setupTasks = listOf(
                    { setupLocalStackSES(localStackEndpoint) },
                    { setupLocalStackScheduler(localStackEndpoint) }
                )

                setupTasks.forEach { task ->
                    try {
                        task()
                    } catch (e: Exception) {
                        logger.error("Failed to setup AWS service", e)
                    }
                }
            }
        }

        private fun setupLocalStackSES(endpoint: String) {
            try {
                logger.info("Setting up LocalStack SES at $endpoint")

                // Use AWS SDK to set up SES programmatically with retry mechanism
                val awsCredentials = BasicAWSCredentials(awsConfig.accessKey, awsConfig.secretKey)
                val awsCredentialsProvider = AWSStaticCredentialsProvider(awsCredentials)

                val sesClient = AmazonSimpleEmailServiceClientBuilder
                    .standard()
                    .withCredentials(awsCredentialsProvider)
                    .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration(endpoint, awsConfig.region)
                    )
                    .build()

                // Wait for LocalStack SES to be ready with exponential backoff
                waitForSESReady(sesClient, maxRetries = awsConfig.ses.readyWaitRetryCount)

                // Setup SES identities from configuration
                val verificationResults = awsConfig.ses.verifiedEmails.map { email ->
                    retryOperation(maxRetries = awsConfig.ses.setupRetryCount) {
                        sesClient.verifyEmailIdentity(
                            VerifyEmailIdentityRequest()
                                .withEmailAddress(email)
                        )
                        logger.info("✓ Verified SES identity: $email")
                        true
                    }
                }

                // Create configuration set with retry
                retryOperation(maxRetries = awsConfig.ses.setupRetryCount) {
                    sesClient.createConfigurationSet(
                        CreateConfigurationSetRequest()
                            .withConfigurationSet(
                                ConfigurationSet()
                                    .withName(awsConfig.ses.configurationSet)
                            )
                    )
                    logger.info("✓ Created SES configuration set: ${awsConfig.ses.configurationSet}")
                    true
                }

                sesClient.shutdown()
                val successfulVerifications = verificationResults.count { it }
                logger.info("✅ LocalStack SES setup completed successfully. Verified $successfulVerifications/${awsConfig.ses.verifiedEmails.size} email identities")
            } catch (e: Exception) {
                logger.error("❌ LocalStack SES setup failed: ${e.message}", e)
                throw RuntimeException("Failed to setup LocalStack SES", e)
            }
        }

        private fun waitForSESReady(sesClient: AmazonSimpleEmailService, maxRetries: Int) {
            repeat(maxRetries) { attempt ->
                try {
                    sesClient.getSendQuota()
                    return
                } catch (e: Exception) {
                    if (attempt == maxRetries - 1) throw e
                    Thread.sleep(1000L * (attempt + 1)) // Exponential backoff
                }
            }
        }

        private fun <T> retryOperation(maxRetries: Int, operation: () -> T): T {
            var lastException: Exception? = null

            repeat(maxRetries) { attempt ->
                try {
                    return operation()
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxRetries - 1) {
                        val delay = if (retryConfig.exponentialBackoff) {
                            retryConfig.initialDelayMs * (attempt + 1)
                        } else {
                            retryConfig.initialDelayMs
                        }
                        Thread.sleep(delay)
                    }
                }
            }

            throw lastException ?: RuntimeException("Operation failed after $maxRetries retries")
        }

        /**
         * LocalStack EventBridge Scheduler 설정 (제한사항 있음)
         * * LocalStack의 EventBridge Scheduler는 기본 API(CreateSchedule, ListSchedules 등)만 지원하며,
         * 실제 스케줄 실행이나 타겟 트리거는 수행하지 않습니다.
         * * 참고: https://docs.localstack.cloud/user-guide/aws/scheduler/
         * - "Only provides mocked functionality"
         * - "Does not actually execute schedules or trigger targets"
         * * 따라서 테스트에서 스케줄러 관련 API 호출 시 500 에러가 발생할 수 있으며,
         * 이는 예상된 동작입니다.
         */
        private fun setupLocalStackScheduler(endpoint: String) {
            try {
                logger.info("Setting up LocalStack EventBridge Scheduler at $endpoint")
                logger.warn("Note: LocalStack EventBridge Scheduler provides mocked functionality only")

                // Validate that EventBridge Scheduler service is accessible
                retryOperation(maxRetries = awsConfig.scheduler.setupRetryCount) {
                    // Since LocalStack Scheduler API is limited, we just verify the endpoint is accessible
                    val httpResponse = URI(endpoint).toURL().openConnection()
                    httpResponse.connect()
                    true
                }

                logger.info("✅ LocalStack EventBridge Scheduler setup completed successfully")
            } catch (e: Exception) {
                logger.error("❌ LocalStack EventBridge Scheduler setup failed: ${e.message}", e)
                // Don't throw exception here as scheduler functionality is mocked anyway
            }
        }
    }
}
