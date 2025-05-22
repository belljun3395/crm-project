package com.manage.crm.config.web.compression

import com.manage.crm.config.web.compression.GzipCompressionUtils.Companion.compressDataBuffer
import com.manage.crm.config.web.compression.GzipCompressionUtils.Companion.readBytes
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.zip.GZIPInputStream

class GzipServerHttpRequest(
    delegate: ServerHttpRequest
) : ServerHttpRequestDecorator(delegate) {

    private val dataBufferFactory = DefaultDataBufferFactory()

    override fun getBody(): Flux<DataBuffer> {
        return super.getBody()
            .map { it.asInputStream(true) }
            .reduce { seq1, seq2 -> SequenceInputStream(seq1, seq2) }
            .handle { compressedStream, sink -> processGzipStream(compressedStream, sink) }
            .flux()
    }

    private fun processGzipStream(compressedStream: InputStream, sink: SynchronousSink<DataBuffer>) {
        try {
            GZIPInputStream(compressedStream).use { decompressedStream ->
                val deflatedBytes = readBytes(decompressedStream)
                val dataBuffer = dataBufferFactory.wrap(deflatedBytes)
                sink.next(dataBuffer)
            }
        } catch (exception: IOException) {
            sink.error(IllegalCompressionRequestException("Failed to decompress gzip content, URI: [${delegate.uri}] due to: ${exception.message}"))
        }
    }
}

class GzipServerHttpResponse(
    delegate: ServerHttpResponse,
    private val minResponseSize: Int
) : ServerHttpResponseDecorator(delegate) {

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        val headers = delegate.headers
        headers.set(HttpHeaders.CONTENT_ENCODING, "gzip")

        return super.writeWith(
            Flux.from(body)
                .collectList()
                .map { dataBuffers ->
                    val dataBufferFactory = delegate.bufferFactory()
                    val allBytes = dataBuffers.flatMap { it.asByteBuffer().array().toList() }

                    // Only compress if the response size is greater than the minimum
                    if (allBytes.size < minResponseSize) {
                        // If response is too small, don't compress
                        dataBufferFactory.wrap(allBytes.toByteArray())
                    } else {
                        delegate.headers.set(HttpHeaders.CONTENT_ENCODING, "gzip")
                        // Compress the response using utility method
                        compressDataBuffer(dataBufferFactory, allBytes.toByteArray())
                    }
                }
                .flux()
        )
    }
}

class GzipServerHttpObject
