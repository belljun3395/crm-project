package com.manage.crm.config.web.compression.properties

data class CompressionUrlProperties(
    var enabled: Boolean = false,
    var patterns: List<String> = emptyList(),
    var minResponseSize: Int = 1024,
    var mimeTypes: List<String> = listOf("application/json", "application/*+json")
)
