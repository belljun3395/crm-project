package com.manage.crm.config.web.compression.gzip

import com.manage.crm.config.web.compression.gzip.GzipCompressionUtils.Companion.isGzipRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Order(Ordered.HIGHEST_PRECEDENCE)
class GzipDecompressionFilter : WebFilter {
    private val log = KotlinLogging.logger {}

    override fun filter(serverWebExchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
        if (!isGzipRequest(serverWebExchange.request)) return webFilterChain.filter(serverWebExchange)
        log.debug { "Detected gzip encoded request, applying decompression" }

        val gzipServerHttpRequest = GzipServerHttpRequest(serverWebExchange.request)
        return serverWebExchange
            .mutate()
            .request(gzipServerHttpRequest)
            .build()
            .let {
                webFilterChain
                    .filter(it)
                    .doOnError { ex: Throwable ->
                        log.error(ex) { "Gzip decompressed HTTP request failed" }
                    }
            }
    }
}
