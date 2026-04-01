package com.manage.crm.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.integration.config.SimpleTestContainers
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.charset.Charset

/**
 * Kotest 기반 통합 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest : DescribeSpec() {
    override fun extensions() = listOf(SpringExtension)

    @LocalServerPort
    private var port: Int = 0

    protected val objectMapper: ObjectMapper = ObjectMapper()
    protected lateinit var webTestClient: WebTestClient

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        beforeSpec {
            webTestClient =
                WebTestClient
                    .bindToServer()
                    .baseUrl("http://localhost:$port")
                    .entityExchangeResultConsumer {
                        val sb = StringBuilder()
                        sb.appendLine()
                        sb.appendLine("================= HTTP Exchange Result ================")
                        sb.appendLine("Request: ${it.method} ${it.url}")
                        sb.appendLine("Request Headers: ${it.requestHeaders}")
                        sb.appendLine("Request Body: ${it.requestBodyContent?.toString(Charset.defaultCharset())}")
                        sb.appendLine()
                        sb.appendLine("Response Status: ${it.status}")
                        sb.appendLine("Response Headers: ${it.responseHeaders}")
                        sb.appendLine("Response Body: ${it.responseBodyContent?.toString(Charset.defaultCharset())}")
                        sb.appendLine("========================================================")
                        logger.info(sb.toString())
                    }.build()
        }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            SimpleTestContainers.register(registry)
        }
    }
}
