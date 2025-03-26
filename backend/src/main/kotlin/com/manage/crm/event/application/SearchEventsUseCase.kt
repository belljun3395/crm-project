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
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.out
import org.springframework.stereotype.Service

@Service
class SearchEventsUseCase(
    private val eventRepository: EventRepository
) {
    suspend fun execute(useCaseIn: SearchEventsUseCaseIn): SearchEventsUseCaseOut {
        val eventName = useCaseIn.eventName
        val propertyOperations = useCaseIn.propertyAndOperations.map { it ->
            val properties = Properties(it.properties.map { Property(it.key, it.value) })
            val operation = it.operation
            val joinOperation = it.joinOperation
            Triple(properties, operation, joinOperation)
        }

        val events = searchEvents(eventName, propertyOperations)

        return out {
            events.map { it ->
                EventDto(
                    it.id!!,
                    it.name!!,
                    it.properties!!.value.map { SearchEventPropertyDto(it.key, it.value) }.toList(),
                    it.createdAt!!
                )
            }.toList().let {
                SearchEventsUseCaseOut(it)
            }
        }
    }

    private suspend fun searchEvents(eventName: String, propertyOperations: List<Triple<Properties, Operation, JoinOperation>>): List<Event> {
        return when {
            propertyOperations.size == 1 -> {
                val (properties, operation) = propertyOperations.first()
                eventRepository.searchByProperty(SearchByPropertyQuery(eventName, properties, operation))
            }

            propertyOperations.size > 1 -> {
                eventRepository.searchByProperties(
                    propertyOperations.map {
                        val (properties, operation, joinOperation) = it
                        SearchByPropertyQuery(eventName, properties, operation, joinOperation)
                    }
                )
            }

            else -> eventRepository.findAllByName(eventName)
        }
    }
}
