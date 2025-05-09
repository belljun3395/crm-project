package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PropertyAndOperationDto
import com.manage.crm.event.application.dto.SearchEventPropertyDto
import com.manage.crm.event.application.dto.SearchEventsUseCaseIn
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class SearchEventsUseCaseTest : BehaviorSpec({
    lateinit var eventRepository: EventRepository
    lateinit var searchEventsUseCase: SearchEventsUseCase

    beforeContainer {
        eventRepository = mockk()
        searchEventsUseCase = SearchEventsUseCase(eventRepository)
    }

    given("SearchEventsUseCase") {
        `when`("search events with one property") {
            val useCaseIn = SearchEventsUseCaseIn(
                eventName = "event",
                propertyAndOperations = listOf(
                    PropertyAndOperationDto(
                        properties = listOf(
                            SearchEventPropertyDto(
                                key = "key",
                                value = "value"
                            )
                        ),
                        operation = Operation.EQUALS,
                        joinOperation = JoinOperation.END
                    )
                )
            )

            val eventSize = 10
            val events = (1..eventSize).map {
                Event.new(
                    id = it.toLong(),
                    name = "event$it",
                    userId = it.toLong(),
                    properties = Properties(
                        listOf(
                            Property("key", "value")
                        )
                    ),
                    createdAt = LocalDateTime.now()
                )
            }
            coEvery { eventRepository.searchByProperty(any(SearchByPropertyQuery::class)) } answers {
                events
            }

            val result = searchEventsUseCase.execute(useCaseIn)
            then("should return SearchEventsUseCaseOut") {
                result.events.size shouldBe eventSize
            }

            then("search events with property") {
                coVerify(exactly = 1) { eventRepository.searchByProperty(any(SearchByPropertyQuery::class)) }
            }
        }

        `when`("search events with multiple properties") {
            val useCaseIn = SearchEventsUseCaseIn(
                eventName = "event",
                propertyAndOperations = listOf(
                    PropertyAndOperationDto(
                        properties = listOf(
                            SearchEventPropertyDto(
                                key = "key1",
                                value = "value1"
                            )
                        ),
                        operation = Operation.EQUALS,
                        joinOperation = JoinOperation.AND
                    ),
                    PropertyAndOperationDto(
                        properties = listOf(
                            SearchEventPropertyDto(
                                key = "key2",
                                value = "value2"
                            )
                        ),
                        operation = Operation.EQUALS,
                        joinOperation = JoinOperation.END
                    )
                )
            )

            val eventSize = 10
            val events = (1..eventSize).map {
                Event.new(
                    id = it.toLong(),
                    name = "event$it",
                    userId = it.toLong(),
                    properties = Properties(
                        listOf(
                            Property("key1", "$it"),
                            Property("key2", "$it")
                        )
                    ),
                    createdAt = LocalDateTime.now()
                )
            }
            coEvery { eventRepository.searchByProperties(any()) } answers {
                events
            }

            val result = searchEventsUseCase.execute(useCaseIn)
            then("should return SearchEventsUseCaseOut") {
                result.events.size shouldBe eventSize
            }

            then("search events with properties") {
                coVerify(exactly = 1) { eventRepository.searchByProperties(any()) }
            }
        }

        `when`("search events without properties") {
            val useCaseIn = SearchEventsUseCaseIn(
                eventName = "event",
                propertyAndOperations = emptyList()
            )

            val eventSize = 10
            val events = (1..eventSize).map {
                Event.new(
                    id = it.toLong(),
                    name = "event$it",
                    userId = it.toLong(),
                    properties = Properties(
                        emptyList()
                    ),
                    createdAt = LocalDateTime.now()
                )
            }
            coEvery { eventRepository.findAllByName(any()) } answers {
                events
            }

            val result = searchEventsUseCase.execute(useCaseIn)
            then("should return SearchEventsUseCaseOut") {
                result.events.size shouldBe eventSize
            }

            then("search events with name") {
                coVerify(exactly = 1) { eventRepository.findAllByName(any()) }
            }
        }
    }
})
