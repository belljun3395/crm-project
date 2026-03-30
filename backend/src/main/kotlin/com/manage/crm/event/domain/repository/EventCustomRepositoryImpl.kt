package com.manage.crm.event.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.event.exception.InvalidSearchConditionException
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.infrastructure.jooq.requireLocalDateTime
import io.r2dbc.postgresql.codec.Json
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class EventCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor,
    private val objectMapper: ObjectMapper
) : EventCustomRepository {

    companion object {
        private const val MAX_SEARCH_CANDIDATE_LIMIT = 1000
    }

    private fun readProperties(row: Map<String, Any>): EventProperties {
        val propertiesJson = when (val value = row["properties"]) {
            is Json -> value.asString()
            else -> value.toString()
        }

        return objectMapper.readValue(propertiesJson, List::class.java).stream()
            .map { objectMapper.convertValue(it, Map::class.java) }
            .map { EventProperty(it["key"] as String, it["value"] as String) }
            .toList()
            .let { EventProperties(it) }
    }

    override suspend fun searchByProperty(query: SearchByPropertyQuery): List<Event> {
        val candidates = if (query.eventName.isBlank()) {
            fetchAll(MAX_SEARCH_CANDIDATE_LIMIT)
        } else {
            fetchAllByEventName(query.eventName)
        }

        return candidates.filter { event -> matchesSingleQuery(event, query) }
    }

    override suspend fun searchByProperties(queries: List<SearchByPropertyQuery>): List<Event> {
        if (queries.isEmpty()) {
            return emptyList()
        }

        val eventName = queries.first().eventName
        if (queries.any { it.eventName != eventName }) {
            throw InvalidSearchConditionException("All queries must have the same eventName")
        }

        val candidates = if (eventName.isNotBlank()) {
            fetchAllByEventName(eventName)
        } else {
            fetchAll(MAX_SEARCH_CANDIDATE_LIMIT)
        }
        return candidates.filter { event -> matchesQueryChain(event, queries) }
    }

    private suspend fun fetchAllByEventName(eventName: String): List<Event> {
        val query = dslContext
            .select()
            .from(CrmJooqTables.Events.table)
            .where(CrmJooqTables.Events.name.eq(eventName))

        return jooqExecutor.fetchList(query) { row ->
            Event.new(
                id = (row["id"] as Number).toLong(),
                name = row["name"] as String,
                userId = (row["user_id"] as Number).toLong(),
                properties = readProperties(row),
                createdAt = row.requireLocalDateTime("created_at")
            )
        }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    private suspend fun fetchAll(limit: Int): List<Event> {
        val query = dslContext
            .select()
            .from(CrmJooqTables.Events.table)
            .orderBy(CrmJooqTables.Events.id.asc())
            .limit(limit)

        return jooqExecutor.fetchList(query) { row ->
            Event.new(
                id = (row["id"] as Number).toLong(),
                name = row["name"] as String,
                userId = (row["user_id"] as Number).toLong(),
                properties = readProperties(row),
                createdAt = row.requireLocalDateTime("created_at")
            )
        }
            .distinctBy { it.id }
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
}
