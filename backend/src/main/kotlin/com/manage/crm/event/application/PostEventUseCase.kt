package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.dto.PostEventUseCaseOut
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.out
import com.manage.crm.user.domain.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class PostEventUseCase(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    suspend fun execute(useCaseIn: PostEventUseCaseIn): PostEventUseCaseOut {
        val eventName = useCaseIn.name
        val externalId = useCaseIn.externalId
        val properties = useCaseIn.properties

        val userId = userRepository.findByExternalId(externalId)?.id
            ?: throw IllegalArgumentException("User not found by externalId: $externalId")

        val savedEvent = eventRepository.save(
            Event(
                name = eventName,
                userId = userId,
                properties = Properties(
                    properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
        )

        return out { PostEventUseCaseOut(savedEvent.id!!) }
    }
}
