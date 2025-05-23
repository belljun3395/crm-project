package com.manage.crm.config.web.compression

import com.manage.crm.config.web.compression.properties.CompressionUrlProperties
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator

abstract class CompressionHttpResponse(
    delegate: ServerHttpResponse,
    private val properties: CompressionUrlProperties,
    private val minResponseSize: Int = properties.minResponseSize
) : ServerHttpResponseDecorator(delegate) {
    fun isMimeTypeMatches(): Boolean {
        return delegate.headers.contentType?.let { type ->
            properties.mimeTypes.any { mt ->
                val mimeType = type.toString()
                mimeType == mt || mimeType.startsWith("$mt;") || mt.endsWith("/*") && mimeType.startsWith(mt.dropLast(1))
            }
        } ?: false
    }

    fun isResponseSizeValid(size: Long): Boolean {
        return size >= minResponseSize
    }
}

class CompressionHttpObject
