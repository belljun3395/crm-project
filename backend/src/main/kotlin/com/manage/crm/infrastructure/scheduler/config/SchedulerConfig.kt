package com.manage.crm.infrastructure.scheduler.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.scheduler.SchedulerClient
import java.net.URI
import java.time.Duration

@Configuration
@EnableScheduling
class SchedulerConfig {
    companion object {
        const val SCHEDULER_CLIENT = "schedulerClient"
    }

    @Value("\${spring.aws.region:#{null}}")
    private val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    private val endpointUrl: String? = null

    @Bean(name = [SCHEDULER_CLIENT])
    @ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws", matchIfMissing = true)
    fun awsSchedulerClient(awsCredentialsProvider: AwsCredentialsProvider): SchedulerClient {
        val overrideConfig = ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofMinutes(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(90))
            .retryStrategy(RetryMode.STANDARD)
            .build()

        val builder = SchedulerClient.builder()
            .overrideConfiguration(overrideConfig)
            .credentialsProvider(awsCredentialsProvider)

        region?.let { builder.region(Region.of(it)) }
        endpointUrl?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }
}
