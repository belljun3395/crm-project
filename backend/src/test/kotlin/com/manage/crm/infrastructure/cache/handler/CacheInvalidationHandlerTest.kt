package com.manage.crm.infrastructure.cache.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate

class CacheInvalidationHandlerTest {
    private val redisTemplate = mockk<RedisTemplate<String, Any>>(relaxed = true)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val handler = CacheInvalidationHandler(redisTemplate, objectMapper)

    @Test
    fun `should invalidate keys from plain JSON message`() {
        // Given
        val message = """{"action":"invalidate", "keys":["user:1", "user:2"]}"""

        // When
        handler.handle(message, isSnsWrapped = false)

        // Then
        verify(exactly = 1) { redisTemplate.delete(listOf("user:1", "user:2")) }
    }

    @Test
    fun `should invalidate keys from SNS wrapped message`() {
        // Given
        val innerMessage = """{"action":"invalidate", "keys":["campaign:100"]}"""
        val snsWrappedMessage = """
            {
                "Type": "Notification",
                "MessageId": "test-id",
                "Message": ${objectMapper.writeValueAsString(innerMessage)}
            }
        """.trimIndent()

        // When
        handler.handle(snsWrappedMessage, isSnsWrapped = true)

        // Then
        verify(exactly = 1) { redisTemplate.delete(listOf("campaign:100")) }
    }

    @Test
    fun `should log warning and skip when action is not invalidate`() {
        // Given
        val message = """{"action":"unknown", "keys":["test"]}"""

        // When
        handler.handle(message, isSnsWrapped = false)

        // Then
        verify(exactly = 0) { redisTemplate.delete(any<List<String>>()) }
    }
}
