package com.manage.crm.config.web.compression.gzip

import com.manage.crm.config.web.compression.CompressionHttpResponse
import com.manage.crm.config.web.compression.gzip.GzipCompressionUtils.Companion.GZIP
import com.manage.crm.config.web.compression.gzip.GzipCompressionUtils.Companion.compress
import com.manage.crm.config.web.compression.gzip.GzipCompressionUtils.Companion.decompress
import com.manage.crm.config.web.compression.properties.CompressionUrlProperties
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GzipServerHttpRequest(
    delegate: ServerHttpRequest
) : ServerHttpRequestDecorator(delegate) {

    override fun getBody(): Flux<DataBuffer> {
        return DataBufferUtils.join(super.getBody())
            .flatMapMany { joined ->
                val bytes = readByte(joined)
                Flux.just(decompress(bytes))
            }
    }
}

class GzipServerHttpResponse(
    delegate: ServerHttpResponse,
    properties: CompressionUrlProperties
) : CompressionHttpResponse(delegate, properties) {
    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        // Skip if the response is not gzip compatible
        if (!isMimeTypeMatches()) {
            return super.writeWith(body)
        }

        return Mono.defer {
            DataBufferUtils.join(body)
                .flatMap { joined ->
                    val bytes = readByte(joined)
                    val buffer = if (isResponseSizeValid(bytes.size.toLong())) {
                        // Remove any existing Content-Length header to enable chunked transfer
                        delegate.headers.remove(HttpHeaders.CONTENT_LENGTH)
                        // Set the Content-Encoding header to indicate that the response is compressed
                        delegate.headers[HttpHeaders.CONTENT_ENCODING] = listOf(GZIP)
                        compress(delegate.bufferFactory(), bytes)
                    } else {
                        delegate.bufferFactory().wrap(bytes)
                    }
                    super.writeWith(Mono.just(buffer))
                }
        }
    }
}

// read the byte array from the DataBuffer and release the buffer
private fun readByte(buffer: DataBuffer): ByteArray {
    return try {
        val count = buffer.readableByteCount()
        if (count <= 0) return ByteArray(0)
        val bytes = ByteArray(count)
        buffer.read(bytes)
        bytes
    } finally {
        DataBufferUtils.release(buffer)
    }
}
class GzipServerHttpObject
