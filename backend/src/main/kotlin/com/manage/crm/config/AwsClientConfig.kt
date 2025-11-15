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
import java.net.URI

@Configuration
@ConditionalOnProperty(name = ["spring.aws.region"])
@ConditionalOnBean(AWSCredentials::class)
class AwsClientConfig {
    companion object {
        const val SNS_CLIENT = "snsClient"
    }

    @Value("\${spring.aws.region}")
    val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    val endpointUrl: String? = null

    @Bean(name = [SNS_CLIENT])
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

        // Configure endpoint URL for LocalStack
        endpointUrl?.let { url ->
            builder.endpointOverride(URI.create(url))
        }

        return builder.build()
    }
}
