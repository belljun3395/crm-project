package com.manage.crm.config.web.compression

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "server.compression.url")
data class CompressionProperties(
    val enabled: Boolean = false,
    val patterns: List<String> = emptyList(),
    val minResponseSize: Int = 1024,
    val mimeTypes: List<String> = listOf("application/json", "application/*+json")
)
