package com.manage.crm.journey.queue

import com.manage.crm.event.domain.Event
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(JourneyTriggerQueuePublisher::class)
class InMemoryJourneyTriggerQueuePublisher(
    private val processor: JourneyTriggerQueueProcessor
) : JourneyTriggerQueuePublisher {
    private val log = KotlinLogging.logger {}
    private val queue = Channel<JourneyTriggerQueueMessage>(capacity = Channel.UNLIMITED)
    private val consumerJobs = mutableListOf<Job>()

    @PostConstruct
    fun startConsumers() {
        repeat(2) {
            val job = eventListenerCoroutineScope(Dispatchers.IO).launch {
                for (message in queue) {
                    runCatching {
                        processor.process(message)
                    }.onFailure { error ->
                        log.error(error) { "Failed to process in-memory journey trigger message: $message" }
                    }
                }
            }
            consumerJobs.add(job)
        }
    }

    @PreDestroy
    fun stopConsumers() {
        queue.close()
        consumerJobs.forEach { it.cancel() }
    }

    override suspend fun publishEventTrigger(event: Event) {
        val eventId = event.id ?: return
        val result = queue.trySend(
            JourneyTriggerQueueMessage(
                triggerType = JourneyTriggerQueueType.EVENT,
                event = JourneyEventPayload(
                    id = eventId,
                    name = event.name,
                    userId = event.userId,
                    properties = event.properties.value.map { property ->
                        JourneyEventPropertyPayload(
                            key = property.key,
                            value = property.value
                        )
                    },
                    createdAt = event.createdAt
                )
            )
        )
        if (result.isFailure) {
            log.error { "Failed to enqueue in-memory EVENT journey trigger for eventId=$eventId" }
        }
    }

    override suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>?) {
        val result = queue.trySend(
            JourneyTriggerQueueMessage(
                triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT,
                changedUserIds = changedUserIds
            )
        )
        if (result.isFailure) {
            log.error { "Failed to enqueue in-memory SEGMENT_CONTEXT journey trigger" }
        }
    }
}
