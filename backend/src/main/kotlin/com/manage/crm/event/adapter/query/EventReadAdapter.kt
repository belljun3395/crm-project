package com.manage.crm.event.adapter.query

import com.manage.crm.event.application.port.query.EventReadModel
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.EventRepository
import org.springframework.stereotype.Component

@Component
class EventReadAdapter(
    private val eventRepository: EventRepository
) : EventReadPort {
    override suspend fun findAllByIdIn(ids: Collection<Long>): List<EventReadModel> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return eventRepository.findAllByIdIn(ids.toList()).map { it.toReadModel() }
    }

    override suspend fun findAllByUserIdIn(userIds: Collection<Long>): List<EventReadModel> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        return eventRepository.findAllByUserIdIn(userIds.toList()).map { it.toReadModel() }
    }
}

private fun Event.toReadModel(): EventReadModel {
    val eventId = requireNotNull(id) { "Event id cannot be null for query result" }
    return EventReadModel(
        id = eventId,
        userId = userId,
        name = name,
        createdAt = createdAt
    )
}
