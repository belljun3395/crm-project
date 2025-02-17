package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.vo.Json
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient
) : UserRepositoryCustom {

    override suspend fun findByEmail(email: String): User? {
        var selectQuery = """
            SELECT * FROM USERS
        """.trimIndent()
        val whereClause = mutableListOf<String>()
        whereClause.add("user_attributes LIKE '%\"email\": $email%'")
        selectQuery = selectQuery.plus(" WHERE ${whereClause.joinToString(" AND ")}")

        return dataBaseClient.sql(selectQuery)
            .fetch()
            .all()
            .map {
                User(
                    id = it["id"] as Long,
                    externalId = it["external_id"] as String,
                    userAttributes = Json(it["user_attributes"] as String),
                    createdAt = it["created_at"] as LocalDateTime,
                    updatedAt = it["updated_at"] as LocalDateTime
                )
            }.awaitFirst()
    }
}
