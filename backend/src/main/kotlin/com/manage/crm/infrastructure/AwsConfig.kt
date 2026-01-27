package com.manage.crm.infrastructure

import com.amazonaws.auth.AWSCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfig {
    companion object {
        const val AWS_CREDENTIAL_PROVIDER = "awsCredentialProvider"
    }

    @Value("\${spring.aws.credentials.access-key:}")
    val accessKey: String = ""

    @Value("\${spring.aws.credentials.secret-key:}")
    val secretKey: String = ""

    @Bean(name = [AWS_CREDENTIAL_PROVIDER])
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "spring.aws.credentials",
        name = ["access-key", "secret-key"],
        matchIfMissing = false
    )
    fun awsCredentialProvider(): AWSCredentials =
        object : AWSCredentials {
            override fun getAWSAccessKeyId(): String = accessKey
            override fun getAWSSecretKey(): String = secretKey
        }
}
