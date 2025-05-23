package com.manage.crm.config.web.compression.gzip

import com.manage.crm.config.web.compression.IllegalCompressionResponseException
import com.manage.crm.config.web.compression.gzip.GzipCompressionUtils.Companion.isGzipResponseRequired
import com.manage.crm.config.web.compression.properties.CompressionUrlProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class GzipCompressionFilter(
    private val properties: CompressionUrlProperties
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

        // Create a decorated response that will compress the body
        val gzipServerHttpResponse = GzipServerHttpResponse(exchange.response, properties)
        val gzipExchange = exchange.mutate()
            .response(gzipServerHttpResponse)
            .build()

        return chain.filter(gzipExchange)
            .onErrorResume { ex: Throwable ->
                when (ex) {
                    is IllegalCompressionResponseException -> {
                        log.error { "Gzip Compression failed: ${ex.message}" }
                        exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR
                        exchange.response.writeWith(
                            Mono.just(
                                exchange.response.bufferFactory().wrap("Gzip Compression failed ${ex.message}".toByteArray())
                            )
                        )
                    }
                    else -> {
                        log.error { "Gzip Compression failed: ${ex.message}" }
                        Mono.error(ex)
                    }
                }
            }
    }
}
