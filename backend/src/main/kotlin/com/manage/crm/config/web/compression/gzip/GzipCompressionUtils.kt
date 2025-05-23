package com.manage.crm.config.web.compression.gzip

import com.manage.crm.config.web.compression.IllegalCompressionResponseException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.CollectionUtils.isEmpty
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GzipCompressionUtils {
    companion object {
        val log = KotlinLogging.logger {}
        const val GZIP = "gzip"

        fun isGzipRequest(serverHttpRequest: ServerHttpRequest): Boolean {
            return containsGzip(serverHttpRequest, CONTENT_ENCODING).also {
                if (it) {
                    log.debug { "Gzip decompression enabled for request" }
                } else {
                    log.debug { "Gzip decompression not enabled for request" }
                }
            }
        }

        fun isGzipResponseRequired(serverHttpRequest: ServerHttpRequest): Boolean {
            return containsGzip(serverHttpRequest, ACCEPT_ENCODING).also {
                if (it) {
                    log.debug { "Gzip compression enabled for response" }
                } else {
                    log.debug { "Gzip compression not enabled for response" }
                }
            }
        }

        private fun containsGzip(serverHttpRequest: ServerHttpRequest, headerName: String): Boolean {
            val headers: HttpHeaders = serverHttpRequest.headers
            if (!isEmpty(headers)) {
                val header = headers.getFirst(headerName)
                return header?.contains(GZIP) ?: false
            }
            return false
        }

        fun decompress(bytes: ByteArray): DataBuffer {
            log.debug { "Gzip decompression enabled for request" }
            return try {
                ByteArrayInputStream(bytes).use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipInputStream ->
                        DefaultDataBufferFactory.sharedInstance.wrap(decompress(gzipInputStream))
                    }
                }
            } catch (e: IOException) {
                throw IllegalCompressionResponseException("Failed to decompress gzip content: ${e.message}", e)
            }
        }

        private fun decompress(inputStream: InputStream?): ByteArray {
            return inputStream?.let { IOUtils.toByteArray(it) } ?: ByteArray(0)
        }

        fun compress(dataBufferFactory: DataBufferFactory, bytes: ByteArray): DataBuffer {
            log.debug { "Gzip compression enabled for response" }
            return dataBufferFactory.wrap(compress(bytes))
        }

        private fun compress(bytes: ByteArray): ByteArray {
            return try {
                ByteArrayOutputStream().use { outputStream ->
                    GZIPOutputStream(outputStream).use { gzipOutputStream ->
                        gzipOutputStream.write(bytes)
                    }
                    outputStream.toByteArray()
                }
            } catch (e: IOException) {
                throw IllegalCompressionResponseException("Failed to compress data: ${e.message}", e)
            }
        }
    }
}
