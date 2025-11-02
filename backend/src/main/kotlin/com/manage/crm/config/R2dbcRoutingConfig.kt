package com.manage.crm.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono

enum class DataSourceType {
    MASTER,
    REPLICA
}

object DataSourceContextHolder {
    private val context = ThreadLocal<DataSourceType>()

    fun set(type: DataSourceType) {
        context.set(type)
    }

    fun get(): DataSourceType? {
        return context.get()
    }

    fun clear() {
        context.remove()
    }
}

@Configuration
class R2dbcRoutingConfig(private val env: Environment) {

    @Bean
    fun masterConnectionFactory(): ConnectionFactory {
        val url = env.getProperty("MASTER_DATABASE_URL")
            ?: throw IllegalStateException("MASTER_DATABASE_URL environment variable not set")
        return ConnectionFactoryFactory.fromUrl(url)
    }

    @Bean
    fun replicaConnectionFactory(): ConnectionFactory {
        val url = env.getProperty("REPLICA_DATABASE_URL")
            ?: throw IllegalStateException("REPLICA_DATABASE_URL environment variable not set")
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
                    .map { m: org.springframework.transaction.reactive.TransactionSynchronizationManager ->
                        if (m.isCurrentTransactionReadOnly()) DataSourceType.REPLICA else DataSourceType.MASTER
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
            return io.r2dbc.spi.ConnectionFactories.get(url)
        }
    }
}
