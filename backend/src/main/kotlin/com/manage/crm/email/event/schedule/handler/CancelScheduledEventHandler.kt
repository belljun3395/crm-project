package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class CancelScheduledEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository
) {
    private val log = KotlinLogging.logger {}

    /**
     *  - Cancel the scheduled event
     */
    suspend fun handle(event: CancelScheduledEvent) {
        val scheduledEvent = scheduledEventRepository
            .findByEventId(event.scheduledEventId)
            ?.cancel()

        if (scheduledEvent == null) {
            log.warn { "Scheduled event not found by event id: ${event.scheduledEventId}. Skipping cancellation." }
            return
        }

        scheduledEventRepository.save(scheduledEvent)
    }
}
