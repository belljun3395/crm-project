package com.manage.crm.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
class AwsClientConfig(
    private val env: Environment
) {
    @Bean
    fun snsClient(): SnsClient {
        val region = env.getProperty("AWS_REGION") ?: "ap-northeast-2"
        return SnsClient.builder()
            .region(Region.of(region))
            .build()
    }
}
