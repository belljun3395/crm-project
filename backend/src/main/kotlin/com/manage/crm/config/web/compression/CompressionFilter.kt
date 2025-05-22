package com.manage.crm.config.web.compression

import com.manage.crm.config.web.compression.GzipCompressionUtils.Companion.isGzipResponseRequired
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class CompressionFilter(
    private val properties: CompressionProperties
) : WebFilter {
    private val log = KotlinLogging.logger {}
    private val pathMatcher = AntPathMatcher()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        // Skip if URL-based compression is disabled
        if (!properties.enabled) {
            return chain.filter(exchange)
        }

        val request = exchange.request
        val path = request.uri.path

        // Check if the request path matches any of the configured patterns
        val shouldCompress = properties.patterns.any { pattern ->
            pathMatcher.match(pattern, path)
        }

        // Skip if the path doesn't match any pattern or client doesn't accept gzip
        if (!shouldCompress || !isGzipResponseRequired(request)) {
            return chain.filter(exchange)
        }

        // Check if the response content type is in the configured mime types
        val contentType = exchange.response.headers.contentType
        val mimeTypeMatches = contentType?.let { type ->
            properties.mimeTypes.any { mimeType ->
                when {
                    mimeType.endsWith("/*+json") -> type.subtype.endsWith("+json")
                    mimeType.endsWith("/*") -> type.type == mimeType.substringBefore("/*")
                    else -> type.toString() == mimeType
                }
            }
        } ?: false

        if (!mimeTypeMatches) {
            return chain.filter(exchange)
        }

        // Create a decorated response that will compress the body
        val decoratedExchange = exchange.mutate()
            .response(GzipServerHttpResponse(exchange.response, properties.minResponseSize))
            .build()

        return chain.filter(decoratedExchange)
            .onErrorResume { exception: Throwable -> this.logError(exception) }
    }

    private fun logError(exception: Throwable): Mono<Void> {
        log.error { "Compressed HTTP response failed, exception: [{}]".format(exception.message) }
        return Mono.empty()
    }
}
