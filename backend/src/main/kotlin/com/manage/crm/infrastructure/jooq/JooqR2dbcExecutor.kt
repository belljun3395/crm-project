package com.manage.crm.infrastructure.jooq

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.jooq.Param
import org.jooq.Query
import org.jooq.conf.ParamType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component

@Component
class JooqR2dbcExecutor(
    private val databaseClient: DatabaseClient,
) {
    suspend fun <T> fetchList(
        query: Query,
        mapper: (Map<String, Any>) -> T,
    ): List<T> =
        bind(query)
            .fetch()
            .all()
            .map(mapper)
            .collectList()
            .awaitFirst()

    suspend fun <T> fetchOne(
        query: Query,
        mapper: (Map<String, Any>) -> T,
    ): T? =
        bind(query)
            .fetch()
            .one()
            .map(mapper)
            .awaitFirstOrNull()

    suspend fun execute(query: Query): Int =
        bind(query)
            .fetch()
            .rowsUpdated()
            .awaitFirst()
            .toInt()

    private fun bind(query: Query): DatabaseClient.GenericExecuteSpec {
        val sql = toPostgresBindMarkers(query.getSQL(ParamType.INDEXED))
        val bindParams =
            query.params.values
                .filterNot(Param<*>::isInline)
                .take(sql.bindMarkerCount())

        var spec = databaseClient.sql(sql)
        bindParams.forEachIndexed { index, param ->
            val value = param.value
            spec =
                if (value == null) {
                    spec.bindNull(index, param.dataType.type)
                } else {
                    spec.bind(index, value)
                }
        }
        return spec
    }

    private fun toPostgresBindMarkers(sql: String): String {
        val rendered = StringBuilder(sql.length + 16)
        var bindIndex = 1
        var inSingleQuote = false
        var inDoubleQuote = false
        var index = 0

        while (index < sql.length) {
            val char = sql[index]
            when (char) {
                '\'' -> {
                    rendered.append(char)
                    if (!inDoubleQuote) {
                        if (inSingleQuote && index + 1 < sql.length && sql[index + 1] == '\'') {
                            rendered.append(sql[index + 1])
                            index++
                        } else {
                            inSingleQuote = !inSingleQuote
                        }
                    }
                }

                '"' -> {
                    rendered.append(char)
                    if (!inSingleQuote) {
                        inDoubleQuote = !inDoubleQuote
                    }
                }

                '?' -> {
                    if (inSingleQuote || inDoubleQuote) {
                        rendered.append(char)
                    } else {
                        rendered.append('$').append(bindIndex++)
                    }
                }

                else -> rendered.append(char)
            }
            index++
        }

        return rendered.toString()
    }

    private fun String.bindMarkerCount(): Int =
        Regex("""\$(\d+)""")
            .findAll(this)
            .map { it.groupValues[1].toInt() }
            .maxOrNull()
            ?: 0
}
