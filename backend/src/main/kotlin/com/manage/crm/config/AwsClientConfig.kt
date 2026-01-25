package com.manage.crm.config

import com.amazonaws.auth.AWSCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
@ConditionalOnBean(AWSCredentials::class)
class AwsClientConfig {
    companion object {
        const val SNS_CLIENT = "snsClient"
        const val SQS_CLIENT = "sqsClient"
    }

    @Value("\${spring.aws.region:ap-northeast-2}")
    private lateinit var region: String

    @Value("\${spring.aws.endpoint-url:#{null}}")
    private var endpointUrl: String? = null

    @Bean(name = [SNS_CLIENT])
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
    fun snsClient(awsCredentials: AWSCredentials): SnsClient {
        val builder = SnsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsCredentials.awsAccessKeyId,
                        awsCredentials.awsSecretKey
                    )
                )
            )

        endpointUrl?.let { url ->
            builder.endpointOverride(URI.create(url))
        }

        return builder.build()
    }

    @Bean(name = [SQS_CLIENT])
    @ConditionalOnProperty(name = ["message.provider"], havingValue = "aws", matchIfMissing = true)
    fun sqsClient(awsCredentials: AWSCredentials): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        awsCredentials.awsAccessKeyId,
                        awsCredentials.awsSecretKey
                    )
                )
            )

        endpointUrl?.let { url ->
            builder.endpointOverride(URI.create(url))
        }

        return builder.build()
    }
}
