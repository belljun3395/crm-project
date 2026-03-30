package com.manage.crm.integration.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object PostgresContainerSupport {
    private const val dockerApiCompatibilityVersion = "1.44"
    private val postgresService = ComposeTestInfraConfig.service("crm-postgres")

    private val container: PostgreSQLContainer<*> by lazy {
        ensureDockerApiCompatibility()
        PostgreSQLContainer(
            DockerImageName.parse(postgresService.image ?: "postgres:16-alpine")
        )
            .withDatabaseName(postgresService.environment["POSTGRES_DB"] ?: "crm")
            .withUsername(postgresService.environment["POSTGRES_USER"] ?: "postgres")
            .withPassword(postgresService.environment["POSTGRES_PASSWORD"] ?: "postgres")
            .apply { start() }
    }

    private fun ensureDockerApiCompatibility() {
        if (System.getProperty("api.version").isNullOrBlank()) {
            // Docker 29+ rejects the default API version negotiated by Testcontainers 1.x.
            System.setProperty("api.version", dockerApiCompatibilityVersion)
        }
    }

    fun register(registry: DynamicPropertyRegistry) {
        val jdbcUrl = container.jdbcUrl
        val r2dbcUrl = "r2dbc:pool:postgresql://${container.host}:${container.getMappedPort(5432)}/${container.databaseName}"

        registry.add("spring.r2dbc.url") { r2dbcUrl }
        registry.add("spring.r2dbc.username") { container.username }
        registry.add("spring.r2dbc.password") { container.password }
        registry.add("spring.r2dbc.routing.master-url") { r2dbcUrl }
        registry.add("spring.r2dbc.routing.replica-url") { r2dbcUrl }
        registry.add("spring.r2dbc.routing.username") { container.username }
        registry.add("spring.r2dbc.routing.password") { container.password }
        registry.add("spring.datasource.jdbc-url") { jdbcUrl }
        registry.add("spring.datasource.username") { container.username }
        registry.add("spring.datasource.password") { container.password }
        registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
    }
}
