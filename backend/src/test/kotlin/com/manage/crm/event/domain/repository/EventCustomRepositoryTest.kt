package com.manage.crm.event.domain.repository

import com.manage.crm.event.EventModuleTestTemplate
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.event.exception.InvalidSearchConditionException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class EventCustomRepositoryTest(
    private val eventRepository: EventRepository
) : EventModuleTestTemplate() {

    @AfterEach
    fun cleanup() = runTest {
        eventRepository.deleteAll()
    }

    @Test
    fun `searchByProperty equals returns exact matches`() = runTest {
        seed("event", 1L, property("k", "1"))
        seed("event", 2L, property("k", "2"))

        val result = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "event",
                properties = EventProperties(listOf(EventProperty("k", "1"))),
                operation = Operation.EQUALS
            )
        )

        assert(result.size == 1)
        assert(result.first().userId == 1L)
    }

    @Test
    fun `searchByProperty greater than compares numerically`() = runTest {
        seed("event", 1L, property("k", "2"))
        seed("event", 2L, property("k", "10"))

        val result = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "event",
                properties = EventProperties(listOf(EventProperty("k", "9"))),
                operation = Operation.GREATER_THAN
            )
        )

        assert(result.size == 1)
        assert(result.first().properties.value.first { it.key == "k" }.value == "10")
    }

    @Test
    fun `searchByProperty like supports wildcard semantics`() = runTest {
        (1..10).forEach {
            seed("event", it.toLong(), property("k", "value$it"))
        }

        val result = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "event",
                properties = EventProperties(listOf(EventProperty("k", "%e1%"))),
                operation = Operation.LIKE
            )
        )

        assert(result.size == 2)
    }

    @Test
    fun `searchByProperty between filters in range`() = runTest {
        (1..10).forEach {
            seed("event", it.toLong(), property("k", "$it"))
        }

        val result = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "event",
                properties = EventProperties(
                    listOf(
                        EventProperty("k", "3"),
                        EventProperty("k", "6")
                    )
                ),
                operation = Operation.BETWEEN
            )
        )

        assert(result.size == 4)
    }

    @Test
    fun `searchByProperty between rejects different keys`() = runTest {
        seed("event", 1L, property("k", "1"))

        val ex = try {
            eventRepository.searchByProperty(
                SearchByPropertyQuery(
                    eventName = "event",
                    properties = EventProperties(
                        listOf(
                            EventProperty("k1", "1"),
                            EventProperty("k2", "2")
                        )
                    ),
                    operation = Operation.BETWEEN
                )
            )
            null
        } catch (e: InvalidSearchConditionException) {
            e
        }

        assert(ex != null)
        assert(ex.message?.contains("needs same key") == true)
    }

    @Test
    fun `searchByProperties supports join AND and OR`() = runTest {
        seed("event", 1L, property("k1", "1"), property("k2", "9"))
        seed("event", 2L, property("k1", "1"), property("k2", "8"))
        seed("event", 3L, property("k1", "7"), property("k2", "9"))

        val andResult = eventRepository.searchByProperties(
            listOf(
                SearchByPropertyQuery(
                    eventName = "event",
                    properties = EventProperties(listOf(EventProperty("k1", "1"))),
                    operation = Operation.EQUALS,
                    joinOperation = JoinOperation.AND
                ),
                SearchByPropertyQuery(
                    eventName = "event",
                    properties = EventProperties(listOf(EventProperty("k2", "9"))),
                    operation = Operation.EQUALS
                )
            )
        )

        val orResult = eventRepository.searchByProperties(
            listOf(
                SearchByPropertyQuery(
                    eventName = "event",
                    properties = EventProperties(listOf(EventProperty("k1", "7"))),
                    operation = Operation.EQUALS,
                    joinOperation = JoinOperation.OR
                ),
                SearchByPropertyQuery(
                    eventName = "event",
                    properties = EventProperties(listOf(EventProperty("k2", "8"))),
                    operation = Operation.EQUALS
                )
            )
        )

        assert(andResult.map { it.userId }.toSet() == setOf(1L))
        assert(orResult.map { it.userId }.toSet() == setOf(2L, 3L))
    }

    @Test
    fun `searchByProperties validates param count`() = runTest {
        seed("event", 1L, property("k", "1"))

        val ex = try {
            eventRepository.searchByProperties(
                listOf(
                    SearchByPropertyQuery(
                        eventName = "event",
                        properties = EventProperties(
                            listOf(
                                EventProperty("k", "1"),
                                EventProperty("k", "2")
                            )
                        ),
                        operation = Operation.EQUALS
                    )
                )
            )
            null
        } catch (e: InvalidSearchConditionException) {
            e
        }

        assert(ex != null)
    }

    @Test
    fun `searchByProperty injection-like values do not bypass filtering`() = runTest {
        seed("safe_event", 1L, property("k", "value1"))
        seed("safe_event", 2L, property("k", "value2"))

        val valueResult = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "safe_event",
                properties = EventProperties(listOf(EventProperty("k", "value1' OR '1'='1"))),
                operation = Operation.EQUALS
            )
        )

        val nameResult = eventRepository.searchByProperty(
            SearchByPropertyQuery(
                eventName = "safe_event' OR '1'='1",
                properties = EventProperties(listOf(EventProperty("k", "value1"))),
                operation = Operation.EQUALS
            )
        )

        assert(valueResult.isEmpty())
        assert(nameResult.isEmpty())
    }

    private suspend fun seed(eventName: String, userId: Long, vararg properties: EventProperty) {
        eventRepository.save(
            com.manage.crm.event.domain.Event.new(
                name = eventName,
                userId = userId,
                properties = EventProperties(properties.toList())
            )
        )
    }

    private fun property(key: String, value: String) = EventProperty(key = key, value = value)
}
