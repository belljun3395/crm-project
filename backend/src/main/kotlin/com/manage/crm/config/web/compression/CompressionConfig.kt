package com.manage.crm.config.web.compression

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CompressionProperties::class)
class CompressionConfig {

    @Bean
    @ConditionalOnProperty(prefix = "server.compression.url", name = ["enabled"], havingValue = "true")
    fun gzipCompressionFilter(properties: CompressionProperties): CompressionFilter {
        return CompressionFilter(properties)
    }
}
