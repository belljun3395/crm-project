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
import java.math.BigDecimal
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
        if (queries.isEmpty()) {
            return emptyList()
        }

        val eventName = queries.first().eventName
        if (queries.any { it.eventName != eventName }) {
            throw InvalidSearchConditionException("All queries must have the same eventName")
        }

        // Fetch candidate events once, then evaluate query clauses in memory.
        return fetchAllByEventName(eventName)
            .filter { event -> matchesQueryChain(event, queries) }
    }

    private suspend fun fetchAllByEventName(eventName: String): List<Event> {
        val query = """
            SELECT * FROM events WHERE name = :eventName
        """.trimIndent()
        return fetchQuery(query, mapOf("eventName" to eventName))
            .sortedBy { it.id }
    }

    private fun matchesQueryChain(event: Event, queries: List<SearchByPropertyQuery>): Boolean {
        var accumulated = matchesSingleQuery(event, queries.first())
        for (index in 1 until queries.size) {
            val join = queries[index - 1].joinOperation
            val current = matchesSingleQuery(event, queries[index])
            accumulated = when (join) {
                JoinOperation.AND -> accumulated && current
                JoinOperation.OR, JoinOperation.END -> accumulated || current
            }
        }
        return accumulated
    }

    private fun matchesSingleQuery(event: Event, query: SearchByPropertyQuery): Boolean {
        return when (query.operation) {
            Operation.BETWEEN -> matchesBetween(event, query.properties)
            else -> matchesSingleProperty(event, query.operation, query.properties)
        }
    }

    private fun matchesSingleProperty(event: Event, operation: Operation, properties: EventProperties): Boolean {
        if (properties.value.size != operation.paramsCnt) {
            throw InvalidSearchConditionException("Operation ${operation.name} needs ${operation.paramsCnt} params")
        }

        val property = properties.value.firstOrNull()
            ?: throw InvalidSearchConditionException("At least one property is required")
        val values = event.properties.value
            .filter { it.key == property.key }
            .map { it.value }
        if (values.isEmpty()) {
            return false
        }

        return values.any { eventValue ->
            compareValue(eventValue, property.value, operation)
        }
    }

    private fun matchesBetween(event: Event, properties: EventProperties): Boolean {
        if (properties.value.size != Operation.BETWEEN.paramsCnt) {
            throw InvalidSearchConditionException("Operation BETWEEN needs ${Operation.BETWEEN.paramsCnt} params")
        }

        val first = properties.value.first()
        val second = properties.value.last()
        if (first.key != second.key) {
            throw InvalidSearchConditionException("Between operation needs same key. But ${first.key} and ${second.key} are different")
        }

        val values = event.properties.value
            .filter { it.key == first.key }
            .map { it.value }
        if (values.isEmpty()) {
            return false
        }

        return values.any { eventValue ->
            if (isNumeric(eventValue) && first.isNum() && second.isNum()) {
                val numericValue = eventValue.toBigDecimalOrNull() ?: return@any false
                val start = first.value.toBigDecimalOrNull() ?: return@any false
                val end = second.value.toBigDecimalOrNull() ?: return@any false
                numericValue >= start && numericValue <= end
            } else {
                eventValue >= first.value && eventValue <= second.value
            }
        }
    }

    private fun compareValue(eventValue: String, queryValue: String, operation: Operation): Boolean {
        val ordering = compareOrdering(eventValue, queryValue)
        return when (operation) {
            Operation.EQUALS -> ordering == 0
            Operation.NOT_EQUALS -> ordering != 0
            Operation.GREATER_THAN -> ordering?.let { it > 0 } ?: false
            Operation.GREATER_THAN_OR_EQUALS -> ordering?.let { it >= 0 } ?: false
            Operation.LESS_THAN -> ordering?.let { it < 0 } ?: false
            Operation.LESS_THAN_OR_EQUALS -> ordering?.let { it <= 0 } ?: false
            Operation.LIKE -> like(eventValue, queryValue)
            Operation.BETWEEN -> throw InvalidSearchConditionException("BETWEEN is not supported in single value comparison")
        }
    }

    private fun compareOrdering(left: String, right: String): Int? {
        val leftNumeric = isNumeric(left)
        val rightNumeric = isNumeric(right)

        if (leftNumeric != rightNumeric) {
            return null
        }
        if (isNumeric(left) && isNumeric(right)) {
            return toBigDecimal(left).compareTo(toBigDecimal(right))
        }
        return left.compareTo(right)
    }

    private fun like(value: String, pattern: String): Boolean {
        val regex = buildString {
            append("^")
            pattern.forEach { char ->
                when (char) {
                    '%' -> append(".*")
                    '_' -> append('.')
                    else -> append(Regex.escape(char.toString()))
                }
            }
            append("$")
        }.let { Regex(it) }
        return regex.matches(value)
    }

    private fun isNumeric(value: String): Boolean = value.matches(Regex("-?\\d+(\\.\\d+)?"))

    private fun toBigDecimal(value: String): BigDecimal {
        return value.toBigDecimalOrNull()
            ?: throw InvalidSearchConditionException("Numeric value expected but got: $value")
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
