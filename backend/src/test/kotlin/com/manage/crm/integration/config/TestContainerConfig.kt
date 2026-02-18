package com.manage.crm.integration.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

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
                    localstack = LocalStackConfig(),
                    aws = AwsConfig(),
                    retry = RetryConfig()
                )
            )
        }
    }
}

data class TestContainers(
    val localstack: LocalStackConfig,
    val aws: AwsConfig,
    val retry: RetryConfig
)

data class LocalStackConfig(
    val enabled: Boolean = true
)

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
