package com.manage.crm.event.domain.repository

import com.manage.crm.event.EventModuleTestTemplate
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime

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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(listOf(Property("propertyKey", "5"))),
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                Properties(
                                    listOf(
                                        Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                Properties(
                                    listOf(
                                        Property(
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
                            Event(
                                name = "event",
                                externalId = "externalId$it",
                                properties = Properties(
                                    listOf(
                                        Property(
                                            key = "propertyKey",
                                            value = "$it"
                                        )
                                    )
                                ),
                                createdAt = LocalDateTime.now()
                            ).let {
                                eventRepository.save(it)
                            }
                        }

                        // when
                        val result = eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                Properties(
                                    listOf(
                                        Property("propertyKey", "1"),
                                        Property("propertyKey", "5")
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
                            Event(
                                name = "event",
                                externalId = "externalId$it",
                                properties = Properties(
                                    listOf(
                                        Property(
                                            key = "propertyKey",
                                            value = "$it"
                                        )
                                    )
                                ),
                                createdAt = LocalDateTime.now()
                            ).let {
                                eventRepository.save(it)
                            }
                        }

                        // when
                        val property1 = Property("propertyKey1", "1")
                        val property2 = Property("propertyKey2", "5")
                        val exception = assertThrows<IllegalArgumentException> {
                            eventRepository.searchByProperty(
                                SearchByPropertyQuery(
                                    "event",
                                    Properties(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result =
                        eventRepository.searchByProperty(
                            SearchByPropertyQuery(
                                "event",
                                Properties(
                                    listOf(
                                        Property(
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
                        Event(
                            name = "event",
                            externalId = "externalId$it",
                            properties = Properties(
                                listOf(
                                    Property(
                                        key = "propertyKey",
                                        value = "value$it"
                                    )
                                )
                            ),
                            createdAt = LocalDateTime.now()
                        ).let {
                            eventRepository.save(it)
                        }
                    }

                    // when
                    val result = eventRepository.searchByProperty(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
        }
    }

    @Nested
    inner class Multiple_properties {

        @Test
        fun `search events by multiple properties with operation and`() {
            runTest {
                // given
                (1..10).map { it ->
                    Event(
                        name = "event",
                        externalId = "externalId$it",
                        properties = Properties(
                            listOf(
                                Property(
                                    key = "propertyKey1",
                                    value = "$it"
                                ),
                                Property(
                                    key = "propertyKey2",
                                    value = "${10 - it}"
                                )
                            )
                        ),
                        createdAt = LocalDateTime.now()
                    ).let {
                        eventRepository.save(it)
                    }
                }

                // when
                val result = eventRepository.searchByProperties(
                    listOf(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                            Properties(
                                listOf(
                                    Property(
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
                    Event(
                        name = "event",
                        externalId = "externalId$it",
                        properties = Properties(
                            listOf(
                                Property(
                                    key = "propertyKey1",
                                    value = "$it"
                                ),
                                Property(
                                    key = "propertyKey2",
                                    value = "${10 - it}"
                                )
                            )
                        ),
                        createdAt = LocalDateTime.now()
                    ).let {
                        eventRepository.save(it)
                    }
                }

                // when
                val result = eventRepository.searchByProperties(
                    listOf(
                        SearchByPropertyQuery(
                            "event",
                            Properties(
                                listOf(
                                    Property(
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
                            Properties(
                                listOf(
                                    Property(
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
                assert(result[0].externalId == "externalId1")
                assert(result[1].externalId == "externalId8")
            }
        }
    }
}
