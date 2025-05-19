package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import org.springframework.stereotype.Component

@Component
class CancelScheduledEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository
) {
    /**
     *  - Cancel the scheduled event
     */
    suspend fun handle(event: CancelScheduledEvent) {
        val scheduledEvent = (
            scheduledEventRepository
                .findByEventId(event.scheduledEventId)
                ?.cancel()
                ?: throw IllegalStateException("Scheduled event not found by event id: ${event.scheduledEventId}")
            )

        scheduledEventRepository.save(scheduledEvent)
    }
}
