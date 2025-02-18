package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.support.transactional.TransactionTemplates
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class CancelScheduledEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val transactionalTemplates: TransactionTemplates
) {
    /**
     *  - Cancel the scheduled event
     */
    suspend fun handle(event: CancelScheduledEvent) {
        transactionalTemplates.writer.executeAndAwait {
            val scheduledEvent = (
                scheduledEventRepository
                    .findByEventId(event.scheduledEventId)
                    ?.cancel()
                    ?: throw IllegalStateException("Scheduled event not found by event id: ${event.scheduledEventId}")
                )

            scheduledEventRepository.save(scheduledEvent)
        }
    }
}
