package com.manage.crm.integration.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.time.Duration

data class TestContainerConfig(
    val testcontainers: TestContainers
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TestContainerConfig::class.java)
        private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

        fun load(): TestContainerConfig {
            return try {
                val resource = ClassPathResource("test-containers.yml")
                objectMapper.readValue(resource.inputStream)
            } catch (e: Exception) {
                logger.error("Failed to load test-containers.yml, using default configuration", e)
                createDefaultConfig()
            }
        }

        private fun createDefaultConfig(): TestContainerConfig {
            return TestContainerConfig(
                testcontainers = TestContainers(
                    mysql = MySqlConfig(),
                    localstack = LocalStackConfig(),
                    aws = AwsConfig(),
                    retry = RetryConfig()
                )
            )
        }
    }
}

data class TestContainers(
    val mysql: MySqlConfig,
    val localstack: LocalStackConfig,
    val aws: AwsConfig,
    val retry: RetryConfig
)

data class MySqlConfig(
    val image: String = "mysql:8.0",
    @JsonProperty("database-name")
    val databaseName: String = "test_crm",
    val username: String = "test_user",
    val password: String = "test_password",
    @JsonProperty("startup-timeout-minutes")
    val startupTimeoutMinutes: Long = 2,
    @JsonProperty("ready-log-pattern")
    val readyLogPattern: String = ".*ready for connections.*",
    @JsonProperty("ready-log-count")
    val readyLogCount: Int = 2,
    @JsonProperty("network-alias")
    val networkAlias: String = "mysql",
    val reuse: Boolean = true
) {
    fun getStartupTimeout(): Duration = Duration.ofMinutes(startupTimeoutMinutes)
}

data class LocalStackConfig(
    val enabled: Boolean = true,
    val image: String = "localstack/localstack:3.8",
    @JsonProperty("startup-timeout-minutes")
    val startupTimeoutMinutes: Long = 3,
    @JsonProperty("ready-log-pattern")
    val readyLogPattern: String = ".*Ready.*",
    @JsonProperty("ready-log-count")
    val readyLogCount: Int = 1,
    @JsonProperty("network-alias")
    val networkAlias: String = "localstack",
    val reuse: Boolean = true,
    val ports: List<Int> = listOf(4566, 25),
    val environment: Map<String, String> = mapOf(
        "SERVICES" to "ses,sqs,sns,events,iam,scheduler",
        "DEBUG" to "0",
        "AWS_DEFAULT_REGION" to "ap-northeast-2",
        "AWS_ACCESS_KEY_ID" to "test",
        "AWS_SECRET_ACCESS_KEY" to "test",
        "SKIP_INFRA_DOWNLOADS" to "1",
        "SES_ACCEPT_ALL_EMAILS" to "true",
        "SES_VERIFIED_EMAILS" to "test@example.com,notification@example.com,noreply@example.com",
        "SES_CONFIGURATION_SET" to "test-configuration-set",
        "LOCALSTACK_HOST" to "localstack"
    )
) {
    fun getStartupTimeout(): Duration = Duration.ofMinutes(startupTimeoutMinutes)
}

data class AwsConfig(
    val region: String = "ap-northeast-2",
    @JsonProperty("access-key")
    val accessKey: String = "test",
    @JsonProperty("secret-key")
    val secretKey: String = "test",
    val ses: SesConfig = SesConfig(),
    val scheduler: SchedulerConfig = SchedulerConfig()
)

data class SesConfig(
    @JsonProperty("verified-emails")
    val verifiedEmails: List<String> = listOf(
        "test@example.com",
        "notification@example.com",
        "noreply@example.com",
        "admin@example.com",
        "example@example.com"
    ),
    @JsonProperty("configuration-set")
    val configurationSet: String = "test-configuration-set",
    @JsonProperty("setup-retry-count")
    val setupRetryCount: Int = 3,
    @JsonProperty("ready-wait-retry-count")
    val readyWaitRetryCount: Int = 5
)

data class SchedulerConfig(
    @JsonProperty("role-arn")
    val roleArn: String = "arn:aws:iam::000000000000:role/TestRole",
    @JsonProperty("sqs-arn")
    val sqsArn: String = "arn:aws:sqs:ap-northeast-2:000000000000:test-queue",
    @JsonProperty("group-name")
    val groupName: String = "test-group",
    @JsonProperty("setup-retry-count")
    val setupRetryCount: Int = 3
)

data class RetryConfig(
    @JsonProperty("max-retries")
    val maxRetries: Int = 3,
    @JsonProperty("initial-delay-ms")
    val initialDelayMs: Long = 500,
    @JsonProperty("exponential-backoff")
    val exponentialBackoff: Boolean = true
)
