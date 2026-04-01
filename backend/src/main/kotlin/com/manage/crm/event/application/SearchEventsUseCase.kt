package com.manage.crm.event.application

import com.manage.crm.event.application.dto.EventDto
import com.manage.crm.event.application.dto.SearchEventPropertyDto
import com.manage.crm.event.application.dto.SearchEventsUseCaseIn
import com.manage.crm.event.application.dto.SearchEventsUseCaseOut
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.support.out
import com.manage.crm.user.application.port.query.UserReadPort
import org.springframework.stereotype.Component

/**
 * UC-EVENT-003
 * Searches events by name and property conditions.
 *
 * Input: event name with one or more property operation groups.
 * Success: returns matched events with external user ids.
 */
@Component
class SearchEventsUseCase(
    private val eventRepository: EventRepository,
    private val userReadPort: UserReadPort,
) {
    suspend fun execute(useCaseIn: SearchEventsUseCaseIn): SearchEventsUseCaseOut {
        val eventName = useCaseIn.eventName
        val propertyOperations =
            useCaseIn.propertyAndOperations.map { it ->
                val properties = EventProperties(it.properties.map { EventProperty(it.key, it.value) })
                val operation = it.operation
                val joinOperation = it.joinOperation
                Triple(properties, operation, joinOperation)
            }

        val events = searchEvents(eventName, propertyOperations)

        val userIds = events.map { it.userId }.toSet().toList()
        val users = userReadPort.findAllByIdIn(userIds).associateBy { it.id }

        return out {
            events
                .map { it ->
                    EventDto(
                        it.id!!,
                        it.name,
                        it.userId.let { users[it]?.externalId },
                        it.properties.value
                            .map { SearchEventPropertyDto(it.key, it.value) }
                            .toList(),
                        it.createdAt!!,
                    )
                }.toList()
                .let {
                    SearchEventsUseCaseOut(it)
                }
        }
    }

    private suspend fun searchEvents(
        eventName: String,
        propertyOperations: List<Triple<EventProperties, Operation, JoinOperation>>,
    ): List<Event> =
        when {
            propertyOperations.size == 1 -> {
                val (properties, operation) = propertyOperations.first()
                eventRepository.searchByProperty(SearchByPropertyQuery(eventName, properties, operation))
            }

            propertyOperations.size > 1 -> {
                eventRepository.searchByProperties(
                    propertyOperations.map {
                        val (properties, operation, joinOperation) = it
                        SearchByPropertyQuery(eventName, properties, operation, joinOperation)
                    },
                )
            }

            else -> if (eventName.isNotBlank()) eventRepository.findAllByName(eventName) else emptyList()
        }
}
