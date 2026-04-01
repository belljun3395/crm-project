package com.manage.crm.infrastructure.message.config

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI

@Configuration
class MessageConfig {
    companion object {
        const val SQS_ASYNC_CLIENT = "sqsAsyncClient"
        const val SQS_TEMPLATE = "sqsTemplate"
        const val SQS_LISTENER_CONTAINER_FACTORY = "defaultSqsListenerContainerFactory"
    }

    @Value("\${spring.aws.region:#{null}}")
    private val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    private val endpointUrl: String? = null

    @Bean(SQS_ASYNC_CLIENT)
    fun sqsAsyncClient(awsCredentialsProvider: AwsCredentialsProvider): SqsAsyncClient {
        val builder =
            SqsAsyncClient
                .builder()
                .credentialsProvider(awsCredentialsProvider)

        region?.let { builder.region(Region.of(it)) }
        endpointUrl?.let { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }

    @Bean(SQS_LISTENER_CONTAINER_FACTORY)
    fun defaultSqsListenerContainerFactory(sqsAsyncClient: SqsAsyncClient): SqsMessageListenerContainerFactory<Any> =
        SqsMessageListenerContainerFactory
            .builder<Any>()
            .configure { opt -> opt.acknowledgementMode(AcknowledgementMode.MANUAL) }
            .sqsAsyncClient(sqsAsyncClient)
            .build()

    @Bean(SQS_TEMPLATE)
    fun sqsTemplate(sqsAsyncClient: SqsAsyncClient): SqsTemplate = SqsTemplate.newTemplate(sqsAsyncClient)
}
