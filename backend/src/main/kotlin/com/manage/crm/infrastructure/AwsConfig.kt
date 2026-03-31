package com.manage.crm.infrastructure

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
@ConditionalOnProperty(name = ["spring.aws.region"])
class AwsConfig {
    companion object {
        const val AWS_CREDENTIALS_PROVIDER = "awsCredentialsProvider"
        const val SNS_CLIENT = "snsClient"
        const val SQS_CLIENT = "sqsClient"
    }

    @Value("\${spring.aws.credentials.access-key}")
    private lateinit var accessKey: String

    @Value("\${spring.aws.credentials.secret-key}")
    private lateinit var secretKey: String

    @Value("\${spring.aws.region}")
    private lateinit var region: String

    @Value("\${spring.aws.endpoint-url:#{null}}")
    private val endpointUrl: String? = null

    @Bean(name = [AWS_CREDENTIALS_PROVIDER])
    fun awsCredentialsProvider(): AwsCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        )

    @Bean(name = [SNS_CLIENT])
    fun snsClient(awsCredentialsProvider: AwsCredentialsProvider): SnsClient {
        val builder = SnsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)

        endpointUrl?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }

    @Bean(name = [SQS_CLIENT])
    fun sqsClient(awsCredentialsProvider: AwsCredentialsProvider): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(awsCredentialsProvider)

        endpointUrl?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }
}
