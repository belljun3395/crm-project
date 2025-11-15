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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
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
)

@Configuration
@EnableAutoConfiguration(exclude = [RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class])
class RedisConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis.cluster")
    fun redisConfigurationProperties(): RedisConfigurationProperties {
        return RedisConfigurationProperties()
    }

    @Bean
    fun redisSerializer(objectMapper: ObjectMapper): RedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
    }

    @Bean
    fun redisConnectionFactory(rcp: RedisConfigurationProperties): RedisConnectionFactory {
        val redisClusterConfiguration = RedisClusterConfiguration(rcp.nodes!!).apply {
            password = RedisPassword.of(rcp.password!!)
            maxRedirects = rcp.maxRedirects!!
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
    fun reactiveRedisTemplate(redisConnectionFactory: ReactiveRedisConnectionFactory, serializer: RedisSerializer<Any>): ReactiveRedisTemplate<String, Any> {
        val builder = newSerializationContext<String, Any>(StringRedisSerializer())
        val keySerializer = StringRedisSerializer()
        val context = builder
            .key(keySerializer)
            .value(serializer)
            .hashValue(serializer)
            .hashKey(keySerializer)
            .build()
        return ReactiveRedisTemplate(redisConnectionFactory, context)
    }
}
