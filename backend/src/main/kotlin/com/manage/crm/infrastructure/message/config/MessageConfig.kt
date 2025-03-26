package com.manage.crm.infrastructure.message.config

import com.amazonaws.auth.AWSCredentials
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentials
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

    @Bean(SQS_ASYNC_CLIENT)
    fun sqsAsyncClient(awsCredentials: AWSCredentials): SqsAsyncClient =
        SqsAsyncClient
            .builder()
            .credentialsProvider {
                object : AwsCredentials {
                    override fun accessKeyId(): String = awsCredentials.awsAccessKeyId
                    override fun secretAccessKey(): String = awsCredentials.awsSecretKey
                }
            }
            .region(Region.of(region))
            .build()

    @Bean(SQS_LISTENER_CONTAINER_FACTORY)
    fun defaultSqsListenerContainerFactory(awsCredentials: AWSCredentials): SqsMessageListenerContainerFactory<Any> =
        SqsMessageListenerContainerFactory
            .builder<Any>()
            .configure { opt ->
                opt.acknowledgementMode(AcknowledgementMode.MANUAL)
            }
            .sqsAsyncClient(sqsAsyncClient(awsCredentials))
            .build()

    @Bean(SQS_TEMPLATE)
    fun sqsTemplate(awsCredentials: AWSCredentials): SqsTemplate =
        SqsTemplate.newTemplate(sqsAsyncClient(awsCredentials))
}
