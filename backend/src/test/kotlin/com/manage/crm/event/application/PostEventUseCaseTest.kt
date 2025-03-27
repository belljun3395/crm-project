package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime.now

class PostEventUseCaseTest : BehaviorSpec({
    lateinit var eventRepository: EventRepository
    lateinit var userRepository: UserRepository
    lateinit var postEventUseCase: PostEventUseCase

    beforeContainer {
        eventRepository = mockk()
        userRepository = mockk()
        postEventUseCase = PostEventUseCase(eventRepository, userRepository)
    }

    given("PostEventUseCase") {
        `when`("post event") {
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = listOf(
                    PostEventPropertyDto(
                        key = "key1",
                        value = "value1"
                    ),
                    PostEventPropertyDto(
                        key = "key2",
                        value = "value2"
                    )
                )
            )

            val user = User(
                id = 1L,
                externalId = useCaseIn.externalId
            )
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = Event(
                name = useCaseIn.name,
                userId = user.id,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event.apply {
                    id = 1
                    createdAt = now()
                }
            }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe 1
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }
        }
    }
})
