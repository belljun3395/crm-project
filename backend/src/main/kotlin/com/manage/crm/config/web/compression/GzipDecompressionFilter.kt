package com.manage.crm.config.web.compression

import com.manage.crm.config.web.compression.GzipCompressionUtils.Companion.isGzipRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class GzipDecompressionFilter : WebFilter {
    private val log = KotlinLogging.logger {}

    override fun filter(serverWebExchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
        if (!isGzipRequest(serverWebExchange.request)) return webFilterChain.filter(serverWebExchange)

        val mutatedWebExchange = getMutatedWebExchange(serverWebExchange)
        return webFilterChain
            .filter(mutatedWebExchange)
            .onErrorResume { exception: Throwable -> this.logError(exception) }
    }

    private fun getMutatedWebExchange(serverWebExchange: ServerWebExchange): ServerWebExchange {
        val mutatedHttpRequest: ServerHttpRequest = GzipServerHttpRequest(serverWebExchange.request)
        return serverWebExchange
            .mutate()
            .request(mutatedHttpRequest)
            .build()
    }

    private fun logError(exception: Throwable): Mono<Void?> {
        log.error { "Gzip decompressed HTTP request failed, exception: [{}]".format(exception.message) }
        return Mono.empty()
    }
}
