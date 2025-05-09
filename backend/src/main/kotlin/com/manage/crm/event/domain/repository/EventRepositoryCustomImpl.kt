package com.manage.crm.event.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper
) : EventRepositoryCustom {
    override suspend fun searchByProperty(query: SearchByPropertyQuery): List<Event> {
        // TODO 중복된 값이 조회 되지 않도록 수정
        var selectQuery = """
            SELECT * FROM events 
            CROSS JOIN JSON_TABLE(properties, '$[*]' 
                COLUMNS (
                    `key` VARCHAR(255) PATH '$.key',
                    `value` VARCHAR(255) PATH '$.value'
                )            
            ) AS properties
        """.trimIndent()

        selectQuery = selectQuery.plus(" WHERE ")
        if (query.properties.value.size == 1) {
            val property = query.properties.value.first()
            val operation = query.operation
            selectQuery = buildSelectQueryForOnePropertyOperation(operation, property, selectQuery)
        } else if (query.operation == Operation.BETWEEN) {
            val properties = query.properties
            selectQuery = buildQueryForBetweenOperation(properties, selectQuery)
        }

        selectQuery = selectQuery.plus(" AND name = '${query.eventName}'")
        return fetchQuery(selectQuery)
    }

    override suspend fun searchByProperties(queries: List<SearchByPropertyQuery>): List<Event> {
        // TODO queries의 값을 한번에 질의하여 조회하는 방법으로 수정
        val events = mutableSetOf<Event>()
        var joinOperation: JoinOperation? = null
        queries.forEach { query ->
            val event = searchByProperty(query)
            if (joinOperation == null) {
                joinOperation = query.joinOperation
                events.addAll(event)
            } else {
                when (joinOperation!!) {
                    JoinOperation.AND -> {
                        events.retainAll(event.toSet())
                    }
                    JoinOperation.OR -> {
                        events.addAll(event)
                    }
                    JoinOperation.END -> {
                        events.addAll(event)
                    }
                }
                joinOperation = query.joinOperation
            }
        }
        return events.toList()
    }

    private suspend fun fetchQuery(selectQuery: String): List<Event> {
        return dataBaseClient.sql(selectQuery)
            .fetch()
            .all()
            .map { it ->
                Event.new(
                    id = it["id"] as Long,
                    name = it["name"] as String,
                    userId = it["user_id"] as Long,
                    properties = (it["properties"] as String)
                        .let { properties ->
                            objectMapper.readValue(properties, List::class.java).stream()
                                .map { objectMapper.convertValue(it, Map::class.java) }
                                .map { Property(it["key"] as String, it["value"] as String) }
                                .toList()
                                .let { Properties(it) }
                        },
                    createdAt = it["created_at"] as LocalDateTime
                )
            }
            .distinct { it.id!! }
            .collectList()
            .awaitFirst()
    }

    private fun buildSelectQueryForOnePropertyOperation(
        operation: Operation,
        property: Property,
        selectQuery: String
    ): String {
        if (operation.paramsCnt != 1) {
            throw IllegalArgumentException("Operation ${operation.name} needs ${operation.paramsCnt} params")
        }

        val whereClause = generateWhereClause(property, operation)

        return selectQuery.plus("  $whereClause ")
    }

    private fun buildQueryForBetweenOperation(properties: Properties, selectQuery: String): String {
        val whereClause = generateBetweenClause(properties)

        return selectQuery.plus(" $whereClause ")
    }

    private fun generateWhereClause(
        property: Property,
        operation: Operation
    ): String {
        val whereClause = mutableListOf<String>()
        whereClause.add("properties.key = '${property.key}'")
        if (property.isNum()) {
            whereClause.add("properties.value ${operation.value} ${property.value}")
        } else {
            whereClause.add("properties.value ${operation.value} '${property.value}'")
        }
        return whereClause.joinToString(" AND ")
    }

    private fun generateBetweenClause(properties: Properties): String {
        var keyFlag = false
        var keyValue = ""
        var betweenKeyClause = ""
        val andClause = mutableListOf<String>()
        properties.value.forEach { property ->
            if (!keyFlag) {
                betweenKeyClause = "properties.key = '${property.key}'"
                keyFlag = true
                keyValue = property.key
            } else {
                if (keyValue != property.key) {
                    throw IllegalArgumentException("Between operation needs same key. But $keyValue and ${property.key} are different")
                }
            }
            if (property.isNum()) {
                andClause.add(property.value)
            } else {
                andClause.add("'${property.value}'")
            }
        }
        return "$betweenKeyClause AND properties.value BETWEEN ${andClause.joinToString(" AND ")}"
    }
}
