package com.manage.crm.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.newSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableAutoConfiguration(exclude = [RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class])
class RedisConfig {
    @Bean
    fun redisSerializer(objectMapper: ObjectMapper): RedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
    }

    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory(
            RedisStandaloneConfiguration(host, port.toInt())
        )
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
        val context = builder
            .key(StringRedisSerializer())
            .value(serializer)
            .hashKey(StringRedisSerializer())
            .hashValue(serializer)
            .build()
        return ReactiveRedisTemplate(redisConnectionFactory, context)
    }

    // ----------------- Redis -----------------
    @Value("\${spring.data.redis.host}")
    private lateinit var host: String

    @Value("\${spring.data.redis.port}")
    private lateinit var port: String
}
