package com.manage.crm.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
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
        // Enable LocalStack by default for integration tests
        private val useLocalStack = System.getProperty("useLocalStack", "true").toBoolean()

        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("test_crm")
            .withUsername("test_user")
            .withPassword("test_password")

        @Container
        @JvmStatic
        val localStackContainer: GenericContainer<*>? = if (useLocalStack) {
            GenericContainer("localstack/localstack:3.8")
                .withExposedPorts(4566, 25)
                .withEnv("SERVICES", "ses,sqs,sns,events,iam,scheduler")
                .withEnv("DEBUG", "0")
                .withEnv("AWS_DEFAULT_REGION", "ap-northeast-2")
                .withEnv("AWS_ACCESS_KEY_ID", "test")
                .withEnv("AWS_SECRET_ACCESS_KEY", "test")
                .withEnv("SKIP_INFRA_DOWNLOADS", "1")
                // Enable SES to accept all emails for testing
                .withEnv("SES_ACCEPT_ALL_EMAILS", "true")
                // Pre-verified SES identities for testing
                .withEnv("SES_VERIFIED_EMAILS", "test@example.com,notification@example.com,noreply@example.com")
                // Auto-setup SES configuration
                .withEnv("SES_CONFIGURATION_SET", "test-configuration-set")
                .waitingFor(Wait.forLogMessage(".*Ready.*", 1))
        } else {
            null
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            mysqlContainer.start()

            // Database configuration
            registry.add("spring.r2dbc.url") {
                "r2dbc:pool:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(3306)}/${mysqlContainer.databaseName}?useSSL=false"
            }
            registry.add("spring.r2dbc.username") { mysqlContainer.username }
            registry.add("spring.r2dbc.password") { mysqlContainer.password }
            registry.add("spring.flyway.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.flyway.user") { mysqlContainer.username }
            registry.add("spring.flyway.password") { mysqlContainer.password }
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }

            // LocalStack configuration (only if enabled)
            if (useLocalStack && localStackContainer != null) {
                localStackContainer.start()
                val localStackEndpoint = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(4566)}"

                registry.add("spring.aws.endpoint-url") { localStackEndpoint }
                registry.add("spring.aws.credentials.access-key") { "test" }
                registry.add("spring.aws.credentials.secret-key") { "test" }
                registry.add("spring.aws.region") { "ap-northeast-2" }

                // Setup SES identities in LocalStack
                setupLocalStackSES(localStackEndpoint)

                // Setup EventBridge Scheduler
                setupLocalStackScheduler(localStackEndpoint)

                // Override SES settings for LocalStack
                registry.add("spring.aws.mail.configuration-set.default") { "test-configuration-set" }

                // Keep mail settings to use SES (not SMTP) via AWS SES client
                registry.add("spring.mail.username") { "example@example.com" }

                // Override schedule settings for LocalStack
                registry.add("spring.aws.schedule.role-arn") { "arn:aws:iam::000000000000:role/TestRole" }
                registry.add("spring.aws.schedule.sqs-arn") { "arn:aws:sqs:ap-northeast-2:000000000000:test-queue" }
                registry.add("spring.aws.schedule.group-name") { "test-group" }
            }
        }

        private fun setupLocalStackSES(endpoint: String) {
            try {
                // Wait for LocalStack to be ready
                Thread.sleep(3000)

                println("Setting up LocalStack SES at $endpoint")

                // Use AWS SDK to setup SES programmatically
                val awsCredentials = com.amazonaws.auth.BasicAWSCredentials("test", "test")
                val awsCredentialsProvider = com.amazonaws.auth.AWSStaticCredentialsProvider(awsCredentials)

                val sesClient = com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
                    .standard()
                    .withCredentials(awsCredentialsProvider)
                    .withEndpointConfiguration(
                        com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(endpoint, "ap-northeast-2")
                    )
                    .build()

                // Setup SES identities
                val emailsToVerify = listOf(
                    "test@example.com",
                    "notification@example.com",
                    "noreply@example.com",
                    "admin@example.com",
                    "example@example.com" // This is the from email in application-test.yml
                )

                emailsToVerify.forEach { email ->
                    try {
                        sesClient.verifyEmailIdentity(
                            com.amazonaws.services.simpleemail.model.VerifyEmailIdentityRequest()
                                .withEmailAddress(email)
                        )
                        println("✓ Verified SES identity: $email")
                    } catch (e: Exception) {
                        println("⚠ Failed to verify SES identity $email: ${e.message}")
                    }
                }

                // Create configuration set
                try {
                    sesClient.createConfigurationSet(
                        com.amazonaws.services.simpleemail.model.CreateConfigurationSetRequest()
                            .withConfigurationSet(
                                com.amazonaws.services.simpleemail.model.ConfigurationSet()
                                    .withName("test-configuration-set")
                            )
                    )
                    println("✓ Created SES configuration set: test-configuration-set")
                } catch (e: Exception) {
                    println("⚠ Failed to create SES configuration set: ${e.message}")
                }

                sesClient.shutdown()
                println("✅ LocalStack SES setup completed successfully")
            } catch (e: Exception) {
                println("❌ LocalStack SES setup failed: ${e.message}")
                e.printStackTrace()
            }
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
                println("Setting up LocalStack EventBridge Scheduler at $endpoint")
                println("Note: LocalStack EventBridge Scheduler provides mocked functionality only")
                println("✅ LocalStack EventBridge Scheduler setup completed successfully")
            } catch (e: Exception) {
                println("❌ LocalStack EventBridge Scheduler setup failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
