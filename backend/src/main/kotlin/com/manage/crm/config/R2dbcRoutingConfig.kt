package com.manage.crm.config

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono

enum class DataSourceType {
    MASTER,
    REPLICA
}

@Configuration
class R2dbcRoutingConfig {

    @Value("\${spring.r2dbc.routing.master-url}")
    val masterDatabaseUrl: String? = null

    @Value("\${spring.r2dbc.routing.replica-url}")
    val replicaDatabaseUrl: String? = null

    @Value("\${spring.r2dbc.routing.username:}")
    val username: String? = null

    @Value("\${spring.r2dbc.routing.password:}")
    val password: String? = null

    @Bean
    fun masterConnectionFactory(): ConnectionFactory {
        val url = masterDatabaseUrl
            ?: throw IllegalStateException("spring.r2dbc.routing.master-url property not set")
        return ConnectionFactoryFactory.create(url, username, password)
    }

    @Bean
    fun replicaConnectionFactory(): ConnectionFactory {
        val url = replicaDatabaseUrl
            ?: throw IllegalStateException("spring.r2dbc.routing.replica-url property not set")
        return ConnectionFactoryFactory.create(url, username, password)
    }

    @Bean
    @Primary
    fun routingConnectionFactory(
        masterConnectionFactory: ConnectionFactory,
        replicaConnectionFactory: ConnectionFactory
    ): ConnectionFactory {
        val routingConnectionFactory = object : AbstractRoutingConnectionFactory() {
            override fun determineCurrentLookupKey(): Mono<Any> {
                return TransactionSynchronizationManager
                    .forCurrentTransaction()
                    .map { m: TransactionSynchronizationManager ->
                        if (m.isCurrentTransactionReadOnly) DataSourceType.REPLICA else DataSourceType.MASTER
                    }
                    .onErrorResume { Mono.just(DataSourceType.MASTER) }
                    .defaultIfEmpty(DataSourceType.MASTER)
                    .cast(Any::class.java)
            }
        }

        routingConnectionFactory.setTargetConnectionFactories(
            mapOf(
                DataSourceType.MASTER to masterConnectionFactory,
                DataSourceType.REPLICA to replicaConnectionFactory
            )
        )
        routingConnectionFactory.setDefaultTargetConnectionFactory(masterConnectionFactory)
        return routingConnectionFactory
    }

    // Helper to create ConnectionFactory from URL with separate credentials
    private object ConnectionFactoryFactory {
        fun create(url: String, username: String?, password: String?): ConnectionFactory {
            // Parse the base URL options
            val baseOptions = ConnectionFactoryOptions.parse(url)

            // Build new options with credentials if provided
            val builder = ConnectionFactoryOptions.builder().from(baseOptions)

            username?.takeIf { it.isNotBlank() }?.let {
                builder.option(ConnectionFactoryOptions.USER, it)
            }

            password?.takeIf { it.isNotBlank() }?.let {
                builder.option(ConnectionFactoryOptions.PASSWORD, it)
            }

            return ConnectionFactories.get(builder.build())
        }
    }
}
