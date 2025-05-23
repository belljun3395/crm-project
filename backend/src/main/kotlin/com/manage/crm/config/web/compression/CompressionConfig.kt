package com.manage.crm.config.web.compression

import com.manage.crm.config.web.compression.gzip.GzipCompressionFilter
import com.manage.crm.config.web.compression.gzip.GzipDecompressionFilter
import com.manage.crm.config.web.compression.properties.CompressionUrlProperties
import com.manage.crm.config.web.compression.properties.DecompressionRequestProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CompressionConfig {
    @Bean
    @ConfigurationProperties(prefix = "server.compression.url")
    fun compressionUrlProperties(): CompressionUrlProperties {
        return CompressionUrlProperties()
    }

    @Bean
    @ConfigurationProperties(prefix = "server.decompression.request")
    fun decompressionRequestProperties(): DecompressionRequestProperties {
        return DecompressionRequestProperties()
    }

    @Bean
    @ConditionalOnProperty(prefix = "server.compression.url", name = ["enabled"], havingValue = "true")
    fun gzipCompressionFilter(properties: CompressionUrlProperties): GzipCompressionFilter {
        return GzipCompressionFilter(properties)
    }

    @Bean
    @ConditionalOnProperty(prefix = "server.decompression.request", name = ["enabled"], havingValue = "true")
    fun gzipDeCompressionFilter(): GzipDecompressionFilter {
        return GzipDecompressionFilter()
    }
}
