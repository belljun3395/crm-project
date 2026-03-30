package com.manage.crm.integration.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object PostgresContainerSupport {
    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test")
            .withUsername("postgres")
            .withPassword("postgres")
            .apply { start() }
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
