package com.manage.crm.support.web.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.support.web.ApiResponse
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "idempotency", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class IdempotencyKeyFilter(
    private val objectMapper: ObjectMapper,
    private val idempotencyRecordStore: IdempotencyRecordStore
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

        return readRequestBody(request).flatMap { requestBodyBytes ->
            val requestHash = IdempotencyKeyPolicy.hashRequestBody(requestBodyBytes)
            val method = request.method.name()
            val path = request.path.value()

            idempotencyRecordStore.get(method, path, idempotencyKey)
                .flatMap { existing -> handleExistingRecord(exchange, existing, requestHash) }
                .switchIfEmpty(
                    idempotencyRecordStore.tryStart(method, path, idempotencyKey, requestHash)
                        .flatMap { started ->
                            if (started) {
                                proceedWithCapture(exchange, chain, idempotencyKey, requestHash, requestBodyBytes)
                            } else {
                                idempotencyRecordStore.get(method, path, idempotencyKey)
                                    .flatMap { existing -> handleExistingRecord(exchange, existing, requestHash) }
                                    .switchIfEmpty(writeConflict(exchange, "Idempotency-Key is already in use"))
                            }
                        }
                )
        }
    }

    private fun handleExistingRecord(
        exchange: ServerWebExchange,
        existing: IdempotencyRecord,
        requestHash: String
    ): Mono<Void> {
        if (existing.requestHash != requestHash) {
            return writeConflict(exchange, "Idempotency-Key is already used with a different request body")
        }

        return when (existing.status) {
            IdempotencyRecordStatus.IN_PROGRESS -> {
                writeConflict(exchange, "Request is already being processed for this Idempotency-Key")
            }

            IdempotencyRecordStatus.COMPLETED -> {
                replay(exchange, existing)
            }
        }
    }

    private fun proceedWithCapture(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
        idempotencyKey: String,
        requestHash: String,
        requestBodyBytes: ByteArray
    ): Mono<Void> {
        val method = exchange.request.method.name()
        val path = exchange.request.path.value()
        val bodyCapture = ByteArrayOutputStream()

        val decoratedRequest = object : ServerHttpRequestDecorator(exchange.request) {
            override fun getBody(): Flux<DataBuffer> {
                return Flux.defer {
                    val buffer = exchange.response.bufferFactory().wrap(requestBodyBytes)
                    Mono.just(buffer)
                }
            }
        }

        val decoratedResponse = object : ServerHttpResponseDecorator(exchange.response) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                val flux = Flux.from(body).map { dataBuffer ->
                    val bytes = ByteArray(dataBuffer.readableByteCount())
                    dataBuffer.read(bytes)
                    DataBufferUtils.release(dataBuffer)
                    bodyCapture.write(bytes)
                    bufferFactory().wrap(bytes)
                }
                return super.writeWith(flux)
            }

            override fun writeAndFlushWith(body: Publisher<out Publisher<out DataBuffer>>): Mono<Void> {
                return writeWith(Flux.from(body).flatMapSequential { it })
            }
        }

        val mutatedExchange = exchange.mutate()
            .request(decoratedRequest)
            .response(decoratedResponse)
            .build()

        return chain.filter(mutatedExchange)
            .onErrorResume { e ->
                idempotencyRecordStore.delete(method, path, idempotencyKey)
                    .then(Mono.error(e))
            }
            .then(
                Mono.defer {
                    val statusCode = decoratedResponse.statusCode?.value() ?: HttpStatus.OK.value()
                    if (statusCode >= 500) {
                        idempotencyRecordStore.delete(method, path, idempotencyKey).then()
                    } else {
                        idempotencyRecordStore.complete(
                            method = method,
                            path = path,
                            key = idempotencyKey,
                            requestHash = requestHash,
                            statusCode = statusCode,
                            responseBody = bodyCapture.toString(StandardCharsets.UTF_8),
                            contentType = decoratedResponse.headers.contentType?.toString()
                        ).then()
                    }
                }
            )
    }

    private fun readRequestBody(request: org.springframework.http.server.reactive.ServerHttpRequest): Mono<ByteArray> {
        return DataBufferUtils.join(request.body)
            .map { dataBuffer ->
                try {
                    ByteArray(dataBuffer.readableByteCount()).also { dataBuffer.read(it) }
                } finally {
                    DataBufferUtils.release(dataBuffer)
                }
            }
            .defaultIfEmpty(ByteArray(0))
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

    private fun writeConflict(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.CONFLICT
        response.headers.contentType = MediaType.APPLICATION_JSON
        val payload = objectMapper.writeValueAsBytes(ApiResponse.FailureBody(message))
        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)))
    }

    private fun replay(exchange: ServerWebExchange, record: IdempotencyRecord): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.resolve(record.statusCode ?: HttpStatus.OK.value()) ?: HttpStatus.OK
        response.headers.contentType = record.contentType?.let { MediaType.parseMediaType(it) } ?: MediaType.APPLICATION_JSON
        val payload = record.responseBody?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0)
        return response.writeWith(Mono.just(response.bufferFactory().wrap(payload)))
    }
}
