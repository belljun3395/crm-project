package com.manage.crm.journey.queue

import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws", matchIfMissing = true)
class InMemoryJourneyTriggerQueuePublisher(
    private val processor: JourneyTriggerQueueProcessor,
    @Value("\${journey.trigger.queue.consumer-count:2}") private val consumerCount: Int,
) : JourneyTriggerQueuePublisher {
    private val log = KotlinLogging.logger {}
    private val queue = Channel<JourneyTriggerQueueMessage>(capacity = QUEUE_CAPACITY)
    private val consumerJobs = mutableListOf<Job>()

    companion object {
        private const val QUEUE_CAPACITY = 1000
    }

    @PostConstruct
    fun startConsumers() {
        repeat(consumerCount) {
            val job =
                eventListenerCoroutineScope(Dispatchers.IO).launch {
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

    override suspend fun publishEventTrigger(event: JourneyEventPayload) {
        val result =
            queue.trySend(
                JourneyTriggerQueueMessage(
                    triggerType = JourneyTriggerQueueType.EVENT,
                    event = event,
                ),
            )
        if (result.isFailure) {
            log.error { "Failed to enqueue in-memory EVENT journey trigger for eventId=${event.id}" }
        }
    }

    override suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>) {
        val result =
            queue.trySend(
                JourneyTriggerQueueMessage(
                    triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT,
                    changedUserIds = changedUserIds,
                ),
            )
        if (result.isFailure) {
            log.error { "Failed to enqueue in-memory SEGMENT_CONTEXT journey trigger" }
        }
    }
}
