package com.manage.crm.event.application

import com.manage.crm.event.application.dto.BrowseEventsUseCaseIn
import com.manage.crm.event.application.dto.BrowseEventsUseCaseOut
import com.manage.crm.event.application.dto.EventDto
import com.manage.crm.event.application.dto.SearchEventPropertyDto
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.out
import com.manage.crm.user.application.port.query.UserReadPort
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * UC-EVENT-002
 * Reads recent events for operations monitoring.
 *
 * Input: optional limit.
 * Success: returns recent events ordered by creation time with external user ids.
 */
@Component
class BrowseEventsUseCase(
    private val eventRepository: EventRepository,
    private val userReadPort: UserReadPort,
) {
    suspend fun execute(useCaseIn: BrowseEventsUseCaseIn): BrowseEventsUseCaseOut {
        val limit = useCaseIn.limit.coerceIn(1, 1000)
        val events =
            eventRepository
                .findAll()
                .toList()
                .sortedByDescending { it.createdAt }
                .take(limit)

        val usersById =
            userReadPort
                .findAllByIdIn(events.map { it.userId }.distinct())
                .associateBy { it.id }

        return out {
            BrowseEventsUseCaseOut(
                events =
                    events.map { event ->
                        EventDto(
                            id = requireNotNull(event.id) { "Event id cannot be null" },
                            name = event.name,
                            externalId = usersById[event.userId]?.externalId,
                            properties = event.properties.value.map { SearchEventPropertyDto(it.key, it.value) },
                            createdAt = event.createdAt ?: LocalDateTime.now(),
                        )
                    },
            )
        }
    }
}
