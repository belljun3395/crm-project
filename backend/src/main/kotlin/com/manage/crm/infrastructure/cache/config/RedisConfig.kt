package com.manage.crm.infrastructure.cache.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.ReadFrom
import io.lettuce.core.SocketOptions
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.internal.HostAndPort
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DnsResolvers
import io.lettuce.core.resource.MappingSocketAddressResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.newSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

data class RedisConfigurationProperties(
    var connectIp: String? = null,
    var nodes: List<String>? = null,
    var password: String? = null,
    var maxRedirects: Int? = null
) {
    fun hasValidClusterNodes(): Boolean = !nodes.isNullOrEmpty() && nodes!!.any { it.isNotBlank() }
}

@Configuration
@EnableAutoConfiguration(exclude = [RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class])
class RedisConfig {
    private val logger = LoggerFactory.getLogger(RedisConfig::class.java)

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis.cluster")
    fun redisConfigurationProperties(): RedisConfigurationProperties {
        return RedisConfigurationProperties()
    }

    @Bean
    fun redisSerializer(objectMapper: ObjectMapper): RedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
    }

    // ----------------- Standalone Redis (for tests and simple deployments) -----------------
    @Bean
    @Primary
    @ConditionalOnProperty(name = ["spring.data.redis.host"])
    fun standaloneRedisConnectionFactory(
        @Value("\${spring.data.redis.host}") host: String,
        @Value("\${spring.data.redis.port:6379}") port: Int
    ): RedisConnectionFactory {
        logger.info("Creating standalone Redis connection factory: {}:{}", host, port)
        val standaloneConfig = RedisStandaloneConfiguration(host, port)
        return LettuceConnectionFactory(standaloneConfig)
    }

    // ----------------- Cluster Redis (for production) -----------------
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory::class)
    fun clusterRedisConnectionFactory(rcp: RedisConfigurationProperties): RedisConnectionFactory {
        if (!rcp.hasValidClusterNodes()) {
            throw IllegalStateException(
                "Redis configuration error: Neither standalone (spring.data.redis.host) " +
                    "nor cluster (spring.data.redis.cluster.nodes) is properly configured"
            )
        }

        logger.info("Creating cluster Redis connection factory with nodes: {}", rcp.nodes)
        val redisClusterConfiguration = RedisClusterConfiguration(rcp.nodes!!).apply {
            rcp.password?.let { password = RedisPassword.of(it) }
            rcp.maxRedirects?.let { maxRedirects = it }
        }

        val socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(100L))
            .keepAlive(true)
            .build()

        val clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .enableAllAdaptiveRefreshTriggers()
            .enablePeriodicRefresh(Duration.ofHours(1L))
            .build()

        val clientOptions = ClusterClientOptions.builder()
            .topologyRefreshOptions(clusterTopologyRefreshOptions)
            .socketOptions(socketOptions)
            .build()

        val resolver = MappingSocketAddressResolver.create(
            DnsResolvers.UNRESOLVED
        ) { hostAndPort -> HostAndPort.of(rcp.connectIp, hostAndPort.port) }

        val clientResources = ClientResources.builder()
            .socketAddressResolver(resolver)
            .build()

        val clientConfiguration = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .clientResources(clientResources)
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build()

        return LettuceConnectionFactory(redisClusterConfiguration, clientConfiguration)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory, serializer: RedisSerializer<Any>): RedisTemplate<String, Any> {
        val redisTemplate = RedisTemplate<String, Any>()
        redisTemplate.connectionFactory = redisConnectionFactory
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.valueSerializer = serializer
        redisTemplate.hashKeySerializer = StringRedisSerializer()
        redisTemplate.hashValueSerializer = serializer
        return redisTemplate
    }

    // ----------------- Reactive -----------------
    @Bean
    fun reactiveRedisConnectionFactory(redisConnectionFactory: RedisConnectionFactory): ReactiveRedisConnectionFactory {
        return redisConnectionFactory as ReactiveRedisConnectionFactory
    }

    @Bean
    fun reactiveRedisTemplate(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory, serializer: RedisSerializer<Any>): ReactiveRedisTemplate<String, Any> {
        val builder = newSerializationContext<String, Any>(StringRedisSerializer())
        val keySerializer = StringRedisSerializer()
        val context = builder
            .key(keySerializer)
            .value(serializer)
            .hashValue(serializer)
            .hashKey(keySerializer)
            .build()
        return ReactiveRedisTemplate(reactiveRedisConnectionFactory, context)
    }

    /**
     * ReactiveRedisTemplate<String, String> for RedisSchedulerProvider
     */
    @Bean
    fun reactiveStringRedisTemplate(reactiveRedisConnectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val keySerializer = StringRedisSerializer()
        val context = newSerializationContext<String, String>(keySerializer)
            .key(keySerializer)
            .value(keySerializer)
            .hashValue(keySerializer)
            .hashKey(keySerializer)
            .build()
        return ReactiveRedisTemplate(reactiveRedisConnectionFactory, context)
    }
}
