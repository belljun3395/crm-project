package com.manage.crm.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.ConfigurationSet
import com.amazonaws.services.simpleemail.model.ConfigurationSetAlreadyExistsException
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
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.nio.charset.Charset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        private val logger = LoggerFactory.getLogger(AbstractIntegrationTest::class.java)

        private val config = TestContainerConfig.load()
        private val localStackConfig = config.testcontainers.localstack
        private val awsConfig = config.testcontainers.aws
        private val retryConfig = config.testcontainers.retry

        private val useLocalStack = System.getProperty("useLocalStack", localStackConfig.enabled.toString()).toBoolean()

        // Spring properties are configured via application-test.yml (docker-compose fixed ports).
        // This method only runs pre-context AWS service setup when LocalStack is enabled.
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            if (useLocalStack) {
                setupAwsServices()
            }
        }

        private fun setupAwsServices() {
            val endpoint = "http://localhost:4566"
            setupLocalStackSES(endpoint) // fatal — 실패 시 예외 전파
            try {
                setupLocalStackScheduler(endpoint) // non-fatal — mocked 기능이므로 실패해도 계속 진행
            } catch (e: Exception) {
                logger.error("Failed to setup AWS scheduler service (non-fatal)", e)
            }
        }

        private fun setupLocalStackSES(endpoint: String) {
            try {
                logger.info("Setting up LocalStack SES at $endpoint")

                val awsCredentials = BasicAWSCredentials(awsConfig.accessKey, awsConfig.secretKey)
                val awsCredentialsProvider = AWSStaticCredentialsProvider(awsCredentials)

                val sesClient = AmazonSimpleEmailServiceClientBuilder
                    .standard()
                    .withCredentials(awsCredentialsProvider)
                    .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration(endpoint, awsConfig.region)
                    )
                    .build()

                waitForSESReady(sesClient, maxRetries = awsConfig.ses.readyWaitRetryCount)

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

                retryOperation(maxRetries = awsConfig.ses.setupRetryCount) {
                    try {
                        sesClient.createConfigurationSet(
                            CreateConfigurationSetRequest()
                                .withConfigurationSet(
                                    ConfigurationSet()
                                        .withName(awsConfig.ses.configurationSet)
                                )
                        )
                        logger.info("✓ Created SES configuration set: ${awsConfig.ses.configurationSet}")
                    } catch (e: ConfigurationSetAlreadyExistsException) {
                        logger.info("SES configuration set already exists: ${awsConfig.ses.configurationSet}")
                    }

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
                    Thread.sleep(1000L * (attempt + 1))
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

                retryOperation(maxRetries = awsConfig.scheduler.setupRetryCount) {
                    val connection = URI(endpoint).toURL().openConnection()
                    try {
                        connection.connect()
                    } finally {
                        (connection as? java.net.HttpURLConnection)?.disconnect()
                    }
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
