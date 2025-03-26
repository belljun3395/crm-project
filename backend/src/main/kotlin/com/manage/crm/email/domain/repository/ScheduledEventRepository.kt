package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.vo.EventId
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ScheduledEventRepository : CoroutineCrudRepository<ScheduledEvent, Long>, ScheduledEventCustomRepository {
    suspend fun findByEventId(eventId: EventId): ScheduledEvent?

    @Query(
        """
        SELECT * FROM scheduled_events 
        WHERE scheduled_events.event_id = :eventId AND scheduled_events.completed = false 
        FOR UPDATE;
    """
    )
    suspend fun findByEventIdAndCompletedFalseForUpdate(eventId: EventId): ScheduledEvent?

    suspend fun findAllByEventClassAndCompletedFalse(eventClass: String): List<ScheduledEvent>

    suspend fun findAllByEventIdIn(eventIds: List<EventId>): List<ScheduledEvent>
}
