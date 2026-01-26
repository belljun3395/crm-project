package com.manage.crm.infrastructure.message.config

import com.amazonaws.auth.AWSCredentials
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
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

    @Value("\${spring.aws.region}")
    private lateinit var region: String

    @Value("\${spring.aws.endpoint-url:#{null}}")
    private var endpointUrl: String? = null

    @Bean(SQS_ASYNC_CLIENT)
    @ConditionalOnBean(AWSCredentials::class)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws")
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

        endpointUrl?.let { url ->
            clientBuilder.endpointOverride(java.net.URI.create(url))
        }

        return clientBuilder.build()
    }

    @Bean(SQS_LISTENER_CONTAINER_FACTORY)
    @ConditionalOnBean(AWSCredentials::class)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws")
    fun defaultSqsListenerContainerFactory(awsCredentials: AWSCredentials): SqsMessageListenerContainerFactory<Any> =
        SqsMessageListenerContainerFactory
            .builder<Any>()
            .configure { opt ->
                opt.acknowledgementMode(AcknowledgementMode.MANUAL)
            }
            .sqsAsyncClient(sqsAsyncClient(awsCredentials))
            .build()

    @Bean(SQS_TEMPLATE)
    @ConditionalOnBean(AWSCredentials::class)
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws")
    fun sqsTemplate(awsCredentials: AWSCredentials): SqsTemplate =
        SqsTemplate.newTemplate(sqsAsyncClient(awsCredentials))
}
