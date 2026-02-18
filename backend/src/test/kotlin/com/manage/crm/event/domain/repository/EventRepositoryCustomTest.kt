package com.manage.crm.event.domain.repository

import com.manage.crm.event.EventModuleTestTemplate
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EventRepositoryCustomTest(
    val eventRepository: EventRepository
) : EventModuleTestTemplate() {
    @AfterEach
    fun cleanup() = runTest {
        eventRepository.deleteAll()
    }

    @Nested
    inner class Property_count_is_one {
        @Nested
        inner class Value_is_number {
            @Test
            fun `search events by equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey",
                                        "1"
                                    )
                                )
                            ),
                            Operation.EQUALS
                        )
                    )

                    // then
                    assert(result.size == 1)
                }
            }

            @Test
            fun `search events by not equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey",
                                        "1"
                                    )
                                )
                            ),
                            Operation.NOT_EQUALS
                        )
                    )

                    // then
                    assert(result.size == 9)
                }
            }

            @Test
            fun `search events by greater than operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(listOf(EventProperty("propertyKey", "5"))),
                            Operation.GREATER_THAN
                        )
                    )

                    // then
                    assert(result.size == 5)
                }
            }

            @Test
            fun `search events by less than operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey",
                                        "5"
                                    )
                                )
                            ),
                            Operation.LESS_THAN
                        )
                    )

                    // then
                    assert(result.size == 4)
                }
            }

            @Test
            fun `search events by greater than or equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                EventProperties(
                                    listOf(
                                        EventProperty(
                                            "propertyKey",
                                            "5"
                                        )
                                    )
                                ),
                                Operation.GREATER_THAN_OR_EQUALS
                            )
                        )

                    // then
                    assert(result.size == 6)
                }
            }

            @Test
            fun `search events by less than or equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                EventProperties(
                                    listOf(
                                        EventProperty(
                                            "propertyKey",
                                            "5"
                                        )
                                    )
                                ),
                                Operation.LESS_THAN_OR_EQUALS
                            )
                        )

                    // then
                    assert(result.size == 5)
                }
            }

            @Nested
            inner class Operation_is_BETWEEN {

                @Test
                fun `search events by between operation`() {
                    runTest {
                        // given
                        (1..10).map { it ->
                            Event.new(
                                name = "event",
                                userId = it.toLong(),
                                properties = EventProperties(
                                    listOf(
                                        EventProperty(
                                            key = "propertyKey",
                                            value = "$it"
                                        )
                                    )
                                )
                            ).let {
                                eventRepository.save(it)
                            }
                        }

                        // when
                        val result = eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                EventProperties(
                                    listOf(
                                        EventProperty("propertyKey", "1"),
                                        EventProperty("propertyKey", "5")
                                    )
                                ),
                                Operation.BETWEEN
                            )
                        )

                        // then
                        assert(result.size == 5)
                    }
                }

                @Test
                fun `search events by between operation with not same property key`() {
                    runTest {
                        // given
                        (1..10).map { it ->
                            Event.new(
                                name = "event",
                                userId = it.toLong(),
                                properties = EventProperties(
                                    listOf(
                                        EventProperty(
                                            key = "propertyKey",
                                            value = "$it"
                                        )
                                    )
                                )
                            ).let {
                                eventRepository.save(it)
                            }
                        }

                        // when
                        val property1 = EventProperty("propertyKey1", "1")
                        val property2 = EventProperty("propertyKey2", "5")
                        val exception = assertThrows<IllegalArgumentException> {
                            eventRepository.searchByProperty(
                                SearchByPropertyQuery(
                                    "event",
                                    EventProperties(
                                        listOf(
                                            property1,
                                            property2
                                        )
                                    ),
                                    Operation.BETWEEN
                                )
                            )
                        }

                        // then
                        assert(exception.message == "Between operation needs same key. But ${property1.key} and ${property2.key} are different")
                    }
                }
            }
        }

        @Nested
        inner class Value_is_string {

            @Test
            fun `search events by equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey",
                                        "value1"
                                    )
                                )
                            ),
                            Operation.EQUALS
                        )
                    )

                    // then
                    assert(result.size == 1)
                }
            }

            @Test
            fun `search events by not equal operation`() {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                EventProperties(
                                    listOf(
                                        EventProperty(
                                            "propertyKey",
                                            "value1"
                                        )
                                    )
                                ),
                                Operation.NOT_EQUALS
                            )
                        )

                    // then
                    assert(result.size == 9)
                }
            }

            @ParameterizedTest
            @CsvSource(
                "value%, 10", // 1~10
                "%1, 1", // 1
                "%e1%, 2" // 1, 10
            )
            fun `search events by like operation`(value: String, expectedSize: Int) {
                runTest {
                    // given
                    (1..10).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            )
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey",
                                        value
                                    )
                                )
                            ),
                            Operation.LIKE
                        )
                    )

                    // then
                    assert(result.size == expectedSize)
                }
            }

            @Test
            fun `search events with SQL injection-like value does not bypass filtering`() {
                runTest {
                    // given
                    (1..3).map { it ->
                        Event.new(
                            name = "event",
                            userId = it.toLong(),
                            properties = EventProperties(
                                listOf(
                                    EventProperty(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            )
                        ).let { eventRepository.save(it) }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty("propertyKey", "value1' OR '1'='1")
                                )
                            ),
                            Operation.EQUALS
                        )
                    )

                    // then
                    assert(result.isEmpty())
                }
            }

            @Test
            fun `search events with SQL injection-like eventName does not bypass filtering`() {
                runTest {
                    // given
                    Event.new(
                        name = "safe_event",
                        userId = 1L,
                        properties = EventProperties(
                            listOf(
                                EventProperty(
                                    key = "propertyKey",
                                    value = "value1"
                                )
                            )
                        )
                    ).let { eventRepository.save(it) }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "safe_event' OR '1'='1",
                            EventProperties(
                                listOf(
                                    EventProperty("propertyKey", "value1")
                                )
                            ),
                            Operation.EQUALS
                        )
                    )

                    // then
                    assert(result.isEmpty())
                }
            }
        }
    }

    @Nested
    inner class Multiple_properties {

        @Test
        fun `search events by multiple properties with operation and`() {
            runTest {
                // given
                (1..10).map { it ->
                    Event.new(
                        name = "event",
                        userId = it.toLong(),
                        properties = EventProperties(
                            listOf(
                                EventProperty(
                                    key = "propertyKey1",
                                    value = "$it"
                                ),
                                EventProperty(
                                    key = "propertyKey2",
                                    value = "${10 - it}"
                                )
                            )
                        )
                    ).let {
                        eventRepository.save(it)
                    }
                }

                // when
                val result = eventRepository.searchByProperties(
                    listOf(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey1",
                                        "5"
                                    )
                                )
                            ),
                            Operation.EQUALS,
                            JoinOperation.AND
                        ),
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey2",
                                        "5"
                                    )
                                )
                            ),
                            Operation.EQUALS
                        )
                    )
                )

                // then
                assert(result.size == 1)
            }
        }

        @Test
        fun `search events by multiple properties with operation or`() {
            runTest {
                // given
                (1..10).map { it ->
                    Event.new(
                        name = "event",
                        userId = it.toLong(),
                        properties = EventProperties(
                            listOf(
                                EventProperty(
                                    key = "propertyKey1",
                                    value = "$it"
                                ),
                                EventProperty(
                                    key = "propertyKey2",
                                    value = "${10 - it}"
                                )
                            )
                        )
                    ).let {
                        eventRepository.save(it)
                    }
                }

                // when
                val result = eventRepository.searchByProperties(
                    listOf(
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey1",
                                        "1"
                                    )
                                )
                            ),
                            Operation.EQUALS,
                            JoinOperation.OR
                        ),
                        SearchByPropertyQuery(
                            "event",
                            EventProperties(
                                listOf(
                                    EventProperty(
                                        "propertyKey2",
                                        "2"
                                    )
                                )
                            ),
                            Operation.EQUALS
                        )
                    )
                )

                // then
                assert(result.size == 2)
                assert(result[0].userId == 1L)
                assert(result[1].userId == 8L)
            }
        }
    }
}
