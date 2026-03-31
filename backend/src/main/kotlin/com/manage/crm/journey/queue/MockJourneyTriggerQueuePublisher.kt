package com.manage.crm.journey.queue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Mock implementation of JourneyTriggerQueuePublisher for testing
 * Stores messages in memory without actual queue processing
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "mock")
class MockJourneyTriggerQueuePublisher : JourneyTriggerQueuePublisher {
    
    private val log = KotlinLogging.logger {}
    private val eventMessages = ConcurrentLinkedQueue<JourneyEventPayload>()
    private val segmentMessages = ConcurrentLinkedQueue<List<Long>>()

    override suspend fun publishEventTrigger(event: JourneyEventPayload) {
        eventMessages.offer(event)
        log.info { "Mock journey trigger: Published event trigger for event ${event.name} (id: ${event.id})" }
    }

    override suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>) {
        segmentMessages.offer(changedUserIds)
        log.info { "Mock journey trigger: Published segment context trigger for ${changedUserIds.size} users" }
    }

    /**
     * Test utility method to get all published event messages
     */
    fun getAllEventMessages(): List<JourneyEventPayload> = eventMessages.toList()

    /**
     * Test utility method to get all published segment messages
     */
    fun getAllSegmentMessages(): List<List<Long>> = segmentMessages.toList()

    /**
     * Test utility method to clear all messages
     */
    fun clearAllMessages() {
        eventMessages.clear()
        segmentMessages.clear()
        log.debug { "Mock journey trigger: Cleared all messages" }
    }

    /**
     * Test utility method to get total message count
     */
    fun getMessageCount(): Int = eventMessages.size + segmentMessages.size
}