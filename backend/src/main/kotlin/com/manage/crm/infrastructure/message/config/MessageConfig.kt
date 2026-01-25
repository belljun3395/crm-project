package com.manage.crm.infrastructure.message.config

import com.amazonaws.auth.AWSCredentials
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
class MessageConfig {
    companion object {
        const val SQS_ASYNC_CLIENT = "sqsAsyncClient"
        const val SQS_TEMPLATE = "sqsTemplate"
        const val SQS_LISTENER_CONTAINER_FACTORY = "defaultSqsListenerContainerFactory"
    }

    // ----------------- AWS SQS -----------------
    @Value("\${spring.aws.region}")
    val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    val endpointUrl: String? = null

    @Bean(SQS_ASYNC_CLIENT)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
    fun sqsAsyncClient(awsCredentials: AWSCredentials): SqsAsyncClient {
        val clientBuilder = SqsAsyncClient
            .builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsCredentials.awsAccessKeyId,
                        awsCredentials.awsSecretKey
                    )
                )
            )
            .region(Region.of(region))

        // Configure endpoint URL for LocalStack
        endpointUrl?.let { url ->
            clientBuilder.endpointOverride(java.net.URI.create(url))
        }

        return clientBuilder.build()
    }

    @Bean(SQS_LISTENER_CONTAINER_FACTORY)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
    fun defaultSqsListenerContainerFactory(awsCredentials: AWSCredentials): SqsMessageListenerContainerFactory<Any> =
        SqsMessageListenerContainerFactory
            .builder<Any>()
            .configure { opt ->
                opt.acknowledgementMode(AcknowledgementMode.MANUAL)
            }
            .sqsAsyncClient(sqsAsyncClient(awsCredentials))
            .build()

    @Bean(SQS_TEMPLATE)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
    fun sqsTemplate(awsCredentials: AWSCredentials): SqsTemplate =
        SqsTemplate.newTemplate(sqsAsyncClient(awsCredentials))
}
