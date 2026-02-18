package com.manage.crm.event.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.event.exception.InvalidSearchConditionException
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * DM-EVENT-001
 * Builds and executes dynamic event-property search queries.
 *
 * Input: event name, property filters, operation, and optional join operation.
 * Success: returns matched events with deduplicated ids.
 * Failure: throws InvalidSearchConditionException for invalid filter contracts.
 * Side effects: reads events from DB using parameter-bound SQL.
 */
@Repository
class EventRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper
) : EventRepositoryCustom {

    private data class QueryClause(
        val clause: String,
        val bindings: Map<String, Any>
    )

    override suspend fun searchByProperty(query: SearchByPropertyQuery): List<Event> {
        var selectQuery = """
            SELECT * FROM events 
            CROSS JOIN JSON_TABLE(properties, '$[*]' 
                COLUMNS (
                    `key` VARCHAR(255) PATH '$.key',
                    `value` VARCHAR(255) PATH '$.value'
                )            
            ) AS properties
        """.trimIndent()

        val queryClause = when (query.operation) {
            Operation.BETWEEN -> buildBetweenClause(query.properties)
            else -> buildSinglePropertyClause(query.operation, query.properties)
        }

        selectQuery = "$selectQuery WHERE ${queryClause.clause} AND name = :eventName"
        return fetchQuery(selectQuery, queryClause.bindings + mapOf("eventName" to query.eventName))
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

    private suspend fun fetchQuery(selectQuery: String, bindings: Map<String, Any>): List<Event> {
        var executeSpec: GenericExecuteSpec = dataBaseClient.sql(selectQuery)
        bindings.forEach { (key, value) ->
            executeSpec = executeSpec.bind(key, value)
        }

        return executeSpec
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
                                .map { EventProperty(it["key"] as String, it["value"] as String) }
                                .toList()
                                .let { EventProperties(it) }
                        },
                    createdAt = it["created_at"] as LocalDateTime
                )
            }
            .distinct { it.id!! }
            .collectList()
            .awaitFirst()
    }

    private fun buildSinglePropertyClause(
        operation: Operation,
        properties: EventProperties
    ): QueryClause {
        if (properties.value.size != operation.paramsCnt) {
            throw InvalidSearchConditionException("Operation ${operation.name} needs ${operation.paramsCnt} params")
        }

        val property = properties.value.first()
        val bindings = mutableMapOf<String, Any>()
        bindings["key0"] = property.key
        val valueKey = "value0"

        val valueClause = if (property.isNum() && operation != Operation.LIKE) {
            bindings[valueKey] = property.value.toBigDecimalOrNull()
                ?: throw InvalidSearchConditionException("Numeric value expected for operation ${operation.name}")
            "CAST(properties.value AS DECIMAL(65, 10)) ${operation.value} :$valueKey"
        } else {
            bindings[valueKey] = property.value
            "properties.value ${operation.value} :$valueKey"
        }

        return QueryClause(
            clause = "properties.key = :key0 AND $valueClause",
            bindings = bindings
        )
    }

    private fun buildBetweenClause(properties: EventProperties): QueryClause {
        if (properties.value.size != Operation.BETWEEN.paramsCnt) {
            throw InvalidSearchConditionException("Operation BETWEEN needs ${Operation.BETWEEN.paramsCnt} params")
        }

        val first = properties.value.first()
        val second = properties.value.last()
        if (first.key != second.key) {
            throw InvalidSearchConditionException("Between operation needs same key. But ${first.key} and ${second.key} are different")
        }

        val bindings = mutableMapOf<String, Any>("betweenKey" to first.key)
        val betweenClause = if (first.isNum() && second.isNum()) {
            bindings["betweenStart"] = first.value.toBigDecimalOrNull()
                ?: throw InvalidSearchConditionException("Numeric value expected for BETWEEN operation")
            bindings["betweenEnd"] = second.value.toBigDecimalOrNull()
                ?: throw InvalidSearchConditionException("Numeric value expected for BETWEEN operation")
            "CAST(properties.value AS DECIMAL(65, 10)) BETWEEN :betweenStart AND :betweenEnd"
        } else {
            bindings["betweenStart"] = first.value
            bindings["betweenEnd"] = second.value
            "properties.value BETWEEN :betweenStart AND :betweenEnd"
        }

        return QueryClause(
            clause = "properties.key = :betweenKey AND $betweenClause",
            bindings = bindings
        )
    }
}
