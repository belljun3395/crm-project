package com.manage.crm.event.application

import com.manage.crm.event.application.dto.BrowseEventsUseCaseIn
import com.manage.crm.event.domain.EventFixtures
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributesFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseEventsUseCaseTest : BehaviorSpec({
    lateinit var eventRepository: EventRepository
    lateinit var userRepository: UserRepository
    lateinit var browseEventsUseCase: BrowseEventsUseCase

    beforeContainer {
        eventRepository = mockk()
        userRepository = mockk()
        browseEventsUseCase = BrowseEventsUseCase(eventRepository, userRepository)
    }

    given("UC-EVENT-002: BrowseEventsUseCase") {
        `when`("limit is within valid range") {
            val events = (1..5).map { i ->
                EventFixtures.giveMeOne()
                    .withId(i.toLong())
                    .withUserId(i.toLong())
                    .withProperties(PropertiesFixtures.giveMeOneEventProperties())
                    .withCreatedAt(LocalDateTime.now().minusMinutes(i.toLong()))
                    .build()
            }
            val users = events.map { e ->
                UserFixtures.giveMeOne()
                    .withId(e.userId)
                    .withExternalId("ext-${e.userId}")
                    .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("{}").build())
                    .build()
            }

            coEvery { eventRepository.findAll() } returns flowOf(*events.toTypedArray())
            coEvery { userRepository.findAllByIdIn(any()) } returns users

            val result = browseEventsUseCase.execute(BrowseEventsUseCaseIn(limit = 3))

            then("returns events capped to requested limit ordered by creation time desc") {
                result.events.size shouldBe 3
                result.events.first().id shouldBe events.first().id
            }

            then("maps external user id onto each event") {
                result.events.forEach { dto ->
                    dto.externalId shouldBe "ext-${dto.id}"
                }
            }
        }

        `when`("limit is below minimum (< 1)") {
            val events = (1..3).map { i ->
                EventFixtures.giveMeOne()
                    .withId(i.toLong())
                    .withUserId(i.toLong())
                    .withProperties(PropertiesFixtures.giveMeOneEventProperties())
                    .withCreatedAt(LocalDateTime.now().minusMinutes(i.toLong()))
                    .build()
            }
            val users = events.map { e ->
                UserFixtures.giveMeOne()
                    .withId(e.userId)
                    .withExternalId("ext-${e.userId}")
                    .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("{}").build())
                    .build()
            }

            coEvery { eventRepository.findAll() } returns flowOf(*events.toTypedArray())
            coEvery { userRepository.findAllByIdIn(any()) } returns users

            val result = browseEventsUseCase.execute(BrowseEventsUseCaseIn(limit = 0))

            then("coerces limit to 1") {
                result.events.size shouldBe 1
            }
        }

        `when`("limit exceeds maximum (> 1000)") {
            val events = (1..5).map { i ->
                EventFixtures.giveMeOne()
                    .withId(i.toLong())
                    .withUserId(i.toLong())
                    .withProperties(PropertiesFixtures.giveMeOneEventProperties())
                    .withCreatedAt(LocalDateTime.now().minusMinutes(i.toLong()))
                    .build()
            }
            val users = events.map { e ->
                UserFixtures.giveMeOne()
                    .withId(e.userId)
                    .withExternalId("ext-${e.userId}")
                    .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("{}").build())
                    .build()
            }

            coEvery { eventRepository.findAll() } returns flowOf(*events.toTypedArray())
            coEvery { userRepository.findAllByIdIn(any()) } returns users

            val result = browseEventsUseCase.execute(BrowseEventsUseCaseIn(limit = 9999))

            then("coerces limit to 1000 and returns all available events") {
                result.events.size shouldBe events.size
            }
        }

        `when`("no events exist") {
            coEvery { eventRepository.findAll() } returns flowOf()
            coEvery { userRepository.findAllByIdIn(any()) } returns emptyList()

            val result = browseEventsUseCase.execute(BrowseEventsUseCaseIn(limit = 10))

            then("returns empty list") {
                result.events shouldBe emptyList()
            }
        }
    }
})
