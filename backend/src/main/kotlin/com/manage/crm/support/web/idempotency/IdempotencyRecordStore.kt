package com.manage.crm.support.web.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class IdempotencyRecordStore(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    @Value("\${idempotency.ttl-seconds:86400}")
    private val ttlSeconds: Long
) {
    fun get(method: String, path: String, key: String): Mono<IdempotencyRecord> {
        return redisTemplate.opsForValue()
            .get(redisKey(method, path, key))
            .map { toRecord(it) }
            .onErrorResume { Mono.empty() }
    }

    fun tryStart(method: String, path: String, key: String, requestHash: String): Mono<Boolean> {
        val record = IdempotencyRecord(
            method = method,
            path = path,
            key = key,
            requestHash = requestHash,
            status = IdempotencyRecordStatus.IN_PROGRESS
        )

        return redisTemplate.opsForValue()
            .setIfAbsent(redisKey(method, path, key), record, Duration.ofSeconds(ttlSeconds))
            .defaultIfEmpty(false)
            .onErrorReturn(false)
    }

    fun complete(
        method: String,
        path: String,
        key: String,
        requestHash: String,
        statusCode: Int,
        responseBody: String,
        contentType: String?
    ): Mono<Boolean> {
        val now = System.currentTimeMillis()
        val record = IdempotencyRecord(
            method = method,
            path = path,
            key = key,
            requestHash = requestHash,
            status = IdempotencyRecordStatus.COMPLETED,
            statusCode = statusCode,
            responseBody = responseBody,
            contentType = contentType,
            createdAt = now,
            updatedAt = now
        )

        return redisTemplate.opsForValue()
            .set(redisKey(method, path, key), record, Duration.ofSeconds(ttlSeconds))
            .defaultIfEmpty(false)
            .onErrorReturn(false)
    }

    fun delete(method: String, path: String, key: String): Mono<Boolean> {
        return redisTemplate.delete(redisKey(method, path, key))
            .map { it > 0 }
            .defaultIfEmpty(false)
            .onErrorReturn(false)
    }

    private fun toRecord(value: Any): IdempotencyRecord {
        return if (value is IdempotencyRecord) {
            value
        } else {
            objectMapper.convertValue(value, IdempotencyRecord::class.java)
        }
    }

    private fun redisKey(method: String, path: String, key: String): String {
        return "idempotency::${method.uppercase()}::$path::$key"
    }
}
