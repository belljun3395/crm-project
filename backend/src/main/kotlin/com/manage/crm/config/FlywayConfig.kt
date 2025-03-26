package com.manage.crm.config

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.function.Consumer

@Configuration
class FlywayConfig {
    companion object {
        const val FLYWAY = "flyway"
        const val FLYWAY_VALIDATE_INITIALIZER = "flywayValidateInitializer"
        const val FLYWAY_MIGRATION_INITIALIZER = "flywayMigrationInitializer"
        const val FLYWAY_PROPERTIES = "flywayProperties"
        const val FLYWAY_CONFIGURATION = "flywayConfiguration"
    }

    @Bean(name = [FLYWAY])
    fun flyway(configuration: org.flywaydb.core.api.configuration.Configuration?): Flyway = Flyway(configuration)

    @Profile("!new")
    @Bean(name = [FLYWAY_VALIDATE_INITIALIZER])
    fun flywayValidateInitializer(flyway: Flyway?): FlywayMigrationInitializer =
        FlywayMigrationInitializer(flyway) { obj: Flyway -> obj.validate() }

    @Bean(name = [FLYWAY_MIGRATION_INITIALIZER])
    fun flywayMigrationInitializer(flyway: Flyway?): FlywayMigrationInitializer =
        FlywayMigrationInitializer(flyway) { obj: Flyway -> obj.migrate() }

    @Bean(name = [FLYWAY_PROPERTIES])
    @ConfigurationProperties(prefix = "spring.flyway")
    fun flywayProperties(): FlywayProperties = FlywayProperties()

    @Bean(name = [FLYWAY_CONFIGURATION])
    fun configuration(): org.flywaydb.core.api.configuration.Configuration {
        val configuration = FluentConfiguration()
        val jdbcUrl = url.replace("r2dbc:pool", "jdbc")
        configuration.dataSource(jdbcUrl, username, password)
        flywayProperties().locations.forEach(
            Consumer { locations: String? ->
                configuration.locations(locations)
            }
        )
        return configuration
    }

    // ----------------- Database -----------------
    @Value("\${spring.r2dbc.url}")
    private lateinit var url: String

    @Value("\${spring.r2dbc.username}")
    private lateinit var username: String

    @Value("\${spring.r2dbc.password}")
    private lateinit var password: String
}
