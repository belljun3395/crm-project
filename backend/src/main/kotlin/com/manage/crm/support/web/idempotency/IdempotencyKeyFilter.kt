package com.manage.crm.support.web.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.support.web.ApiResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "idempotency", name = ["enabled"], havingValue = "true")
class IdempotencyKeyFilter(
    private val objectMapper: ObjectMapper
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        if (!requiresIdempotencyKey(request.method, request.path.value())) {
            return chain.filter(exchange)
        }

        val idempotencyKey = request.headers.getFirst(IdempotencyKeyPolicy.HEADER_NAME)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (idempotencyKey == null) {
            return writeBadRequest(exchange, "Idempotency-Key header is required")
        }

        if (!IdempotencyKeyPolicy.isValid(idempotencyKey)) {
            return writeBadRequest(exchange, "Idempotency-Key format is invalid")
        }

        exchange.attributes[IdempotencyKeyPolicy.EXCHANGE_ATTRIBUTE] = idempotencyKey
        return chain.filter(exchange)
    }

    private fun requiresIdempotencyKey(method: HttpMethod?, path: String): Boolean {
        return when {
            method == HttpMethod.POST && path == "/api/v1/users" -> true
            method == HttpMethod.POST && (path == "/api/v1/events" || path == "/api/v1/events/campaign") -> true
            method == HttpMethod.POST &&
                (path == "/api/v1/emails/send/notifications" || path == "/api/v1/emails/schedules/notifications/email") -> true
            method == HttpMethod.POST && path == "/api/v1/webhooks" -> true
            method == HttpMethod.PUT && path.startsWith("/api/v1/webhooks/") -> true
            else -> false
        }
    }

    private fun writeBadRequest(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.BAD_REQUEST
        response.headers.contentType = MediaType.APPLICATION_JSON
        val payload = objectMapper.writeValueAsBytes(ApiResponse.FailureBody(message))
        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)))
    }
}
