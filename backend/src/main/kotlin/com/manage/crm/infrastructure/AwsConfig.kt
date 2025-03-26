package com.manage.crm.infrastructure

import com.amazonaws.auth.AWSCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AwsConfig {
    companion object {
        const val AWS_CREDENTIAL_PROVIDER = "awsCredentialProvider"
    }

    @Value("\${spring.aws.credentials.access-key}")
    val accessKey: String? = null

    @Value("\${spring.aws.credentials.secret-key}")
    val secretKey: String? = null

    @Bean(name = [AWS_CREDENTIAL_PROVIDER])
    fun awsCredentialProvider(): AWSCredentials =
        object : AWSCredentials {
            override fun getAWSAccessKeyId(): String = accessKey ?: ""
            override fun getAWSSecretKey(): String = secretKey ?: ""
        }
}
