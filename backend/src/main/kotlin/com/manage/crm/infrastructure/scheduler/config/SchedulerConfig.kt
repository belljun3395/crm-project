package com.manage.crm.infrastructure.scheduler.config

import com.amazonaws.auth.AWSCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.scheduler.SchedulerClient
import java.time.Duration

@Configuration
@EnableScheduling
class SchedulerConfig {
    companion object {
        const val SCHEDULER_CLIENT = "schedulerClient"
    }

    // ----------------- AWS Scheduler -----------------
    @Value("\${spring.aws.region}")
    val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    val endpointUrl: String? = null

    @Bean(name = [SCHEDULER_CLIENT])
    fun awsSchedulerClient(
        awsCredentials: AWSCredentials
    ): SchedulerClient {
        val overrideConfig =
            ClientOverrideConfiguration
                .builder()
                .apiCallTimeout(Duration.ofMinutes(2))
                .apiCallAttemptTimeout(Duration.ofSeconds(90))
                .retryStrategy(RetryMode.STANDARD)
                .build()

        val clientBuilder = SchedulerClient
            .builder()
            .region(Region.of(region))
            .overrideConfiguration(overrideConfig)
            .credentialsProvider({
                object : AwsCredentials {
                    override fun accessKeyId(): String = awsCredentials.awsAccessKeyId
                    override fun secretAccessKey(): String = awsCredentials.awsSecretKey
                }
            })

        // Configure endpoint URL for LocalStack
        endpointUrl?.let { url ->
            clientBuilder.endpointOverride(java.net.URI.create(url))
        }

        return clientBuilder.build()
    }
}
