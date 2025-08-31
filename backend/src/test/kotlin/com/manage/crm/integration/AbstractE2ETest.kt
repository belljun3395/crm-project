package com.manage.crm.integration

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
abstract class AbstractE2ETest : DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    @LocalServerPort
    private var port: Int = 0

    protected val objectMapper: ObjectMapper = ObjectMapper()
    protected lateinit var webTestClient: WebTestClient

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        beforeSpec {
            webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:$port")
                .entityExchangeResultConsumer {
                    logger.info("Request: {} {}", it.method, it.url)
                    logger.info("Request Headers: {}", it.requestHeaders)
                    logger.info("Request Body: {}", it.requestBodyContent?.toString(Charset.defaultCharset()))

                    logger.info("Response Status: {}", it.status)
                    logger.info("Response Headers: {}", it.responseHeaders)
                    logger.info("Response Body: {}", it.responseBodyContent?.toString(Charset.defaultCharset()))
                }
                .build()
        }
    }

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("test_crm")
            .withUsername("test_user")
            .withPassword("test_password")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            mysqlContainer.start()
            registry.add("spring.r2dbc.url") {
                "r2dbc:pool:mysql://${mysqlContainer.host}:${mysqlContainer.getMappedPort(3306)}/${mysqlContainer.databaseName}?useSSL=false"
            }
            registry.add("spring.r2dbc.username") { mysqlContainer.username }
            registry.add("spring.r2dbc.password") { mysqlContainer.password }
            registry.add("spring.flyway.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.flyway.user") { mysqlContainer.username }
            registry.add("spring.flyway.password") { mysqlContainer.password }
            registry.add("spring.datasource.url") { mysqlContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
        }
    }
}
