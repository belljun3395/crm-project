package com.manage.crm.config

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
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

    @Bean
    fun masterConnectionFactory(): ConnectionFactory {
        val url = masterDatabaseUrl
            ?: throw IllegalStateException("spring.r2dbc.routing.master-url property not set")
        return ConnectionFactoryFactory.fromUrl(url)
    }

    @Bean
    fun replicaConnectionFactory(): ConnectionFactory {
        val url = replicaDatabaseUrl
            ?: throw IllegalStateException("spring.r2dbc.routing.replica-url property not set")
        return ConnectionFactoryFactory.fromUrl(url)
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

    // Helper to create ConnectionFactory from URL (assuming r2dbc-pool or similar is available)
    // This part might need adjustment based on actual R2DBC driver usage
    private object ConnectionFactoryFactory {
        fun fromUrl(url: String): ConnectionFactory {
            // Example: io.r2dbc.spi.ConnectionFactories.get(url)
            // You might need to use a specific driver\'s ConnectionFactoryBuilder
            // For mysql, it might be something like:
            // return io.r2dbc.mysql.MysqlConnectionFactory.from(io.r2dbc.mysql.MysqlConnectionConfiguration.builder()...)
            // For simplicity, using generic get for now. Ensure r2dbc-pool is in classpath.
            return ConnectionFactories.get(url)
        }
    }
}
