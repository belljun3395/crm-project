package com.manage.crm.infrastructure.cache.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun allKeyGenerator(): KeyGenerator {
        return KeyGenerator { _, _, _ -> "all" }
    }

    // ----------------- Redis -----------------
    @Bean
    fun cacheManager(@Qualifier("redisConnectionFactory") connectionFactory: RedisConnectionFactory, serializer: RedisSerializer<Any>): CacheManager {
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(redisCacheConfiguration(serializer))
            .build()
    }

    fun redisCacheConfiguration(serializer: RedisSerializer<Any>): RedisCacheConfiguration {
        return RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
    }
}
