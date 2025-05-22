package com.manage.crm.config.web.compression

import org.apache.commons.io.IOUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT_ENCODING
import org.springframework.http.HttpHeaders.CONTENT_ENCODING
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.CollectionUtils.isEmpty
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

class GzipCompressionUtils {
    companion object {
        private const val GZIP = "gzip"
        private const val UNKNOWN = "unknown"

        fun getDeflatedBytes(inputStream: InputStream?): ByteArray {
            val string: String = IOUtils.toString(inputStream, UTF_8)
            return string.toByteArray()
        }

        fun compressDataBuffer(dataBufferFactory: DataBufferFactory, bytes: ByteArray): DataBuffer {
            return dataBufferFactory.wrap(compressBytes(bytes))
        }

        private fun compressBytes(bytes: ByteArray): ByteArray {
            try {
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                        gzipOutputStream.write(bytes)
                        gzipOutputStream.finish()
                    }
                    return byteArrayOutputStream.toByteArray()
                }
            } catch (e: IOException) {
                throw IllegalCompressionResponseException("Failed to compress data: ${e.message}")
            }
        }

        fun isGzipRequest(serverHttpRequest: ServerHttpRequest): Boolean {
            return containsGzip(serverHttpRequest, CONTENT_ENCODING)
        }

        fun isGzipResponseRequired(serverHttpRequest: ServerHttpRequest): Boolean {
            return containsGzip(serverHttpRequest, ACCEPT_ENCODING)
        }

        private fun containsGzip(serverHttpRequest: ServerHttpRequest, headerName: String): Boolean {
            val headers: HttpHeaders = serverHttpRequest.headers
            if (!isEmpty(headers)) {
                val header = headers.getFirst(headerName)
                return header?.contains(GZIP) ?: false
            }
            return false
        }
    }
}
