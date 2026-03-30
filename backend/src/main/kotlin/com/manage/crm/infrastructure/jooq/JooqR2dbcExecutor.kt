package com.manage.crm.infrastructure.jooq

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class JooqR2dbcExecutor(
    private val databaseClient: DatabaseClient
) {
    suspend fun <T> fetchList(query: Query, mapper: (Map<String, Any>) -> T): List<T> {
        return bind(query)
            .fetch()
            .all()
            .map(mapper)
            .collectList()
            .awaitFirst()
    }

    suspend fun <T> fetchOne(query: Query, mapper: (Map<String, Any>) -> T): T? {
        return bind(query)
            .fetch()
            .one()
            .map(mapper)
            .awaitFirstOrNull()
    }

    suspend fun execute(query: Query): Int {
        return bind(query)
            .fetch()
            .rowsUpdated()
            .awaitFirst()
            .toInt()
    }

    private fun bind(query: Query): DatabaseClient.GenericExecuteSpec {
        val sql = query.sql
        val bindValues = query.bindValues

        var spec = databaseClient.sql(sql)
        bindValues.forEachIndexed { index, value ->
            spec = if (value == null) {
                spec.bindNull(index, Any::class.java)
            } else {
                spec.bind(index, value)
            }
        }
        return spec
    }
}
